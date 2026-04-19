from google import genai
import os
import eng_to_ipa as ipa
from transformers import WhisperProcessor, WhisperForConditionalGeneration
import torch
import torchaudio
import torch.quantization
import numpy as np
import json
from app.schemas import *
from app.core import settings
import os
from Bio import Align
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(BASE_DIR, "model", "finetuning2")

class PronunciationService:
    def __init__(self):
        if not os.environ.get("GOOGLE_API_KEY"):
            os.environ["GOOGLE_API_KEY"] = settings.GOOGLE_API_KEY
        self.llm = genai.Client()
        
        # Load Whisper model
        model_name = MODEL_PATH
        self.processor = WhisperProcessor.from_pretrained(model_name)
        self.model = WhisperForConditionalGeneration.from_pretrained(model_name)

        # self.model = torch.quantization.quantize_dynamic(
        #         #     self.model, {torch.nn.Linear}, dtype=torch.qint8
        #         # )

        self.model.config.forced_decoder_ids = None
        self.model.generation_config.forced_decoder_ids = None
        self.model.generation_config.suppress_tokens = None
        self.model.config.suppress_tokens = None
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        print('Using device:', self.device)
        self.model.to(self.device)
        
        # IPA mapping dictionary
        self.ipa_to_id = {
            ' ': 1, "'": 2, '*': 3, 'a': 4, 'b': 5, 'c': 6, 'd': 7, 'e': 8, 
            'f': 9, 'g': 10, 'h': 11, 'i': 12, 'j': 13, 'k': 14, 'l': 15, 
            'm': 16, 'n': 17, 'o': 18, 'p': 19, 'q': 20, 'r': 21, 's': 22, 
            't': 23, 'u': 24, 'v': 25, 'w': 26, 'x': 27, 'y': 28, 'z': 29, 
            'æ': 30, 'ð': 31, 'ŋ': 32, 'ɑ': 33, 'ɔ': 34, 'ə': 35, 'ɛ': 36, 
            'ɪ': 37, 'ʃ': 38, 'ʊ': 39, 'ʒ': 40, 'ʤ': 41, 'ʧ': 42, 'ˈ': 43, 
            'ˌ': 44, 'θ': 45
        }
        self.id_to_ipa = {v: k for k, v in self.ipa_to_id.items()}
    
    def convert_whisper_tokens_to_ipa(self, tokens_str):
        """
        Convert Whisper output tokens ("-1-2-3-4") to IPA
        Output: list of dict [{ipa_char: position}, ...]
        """
        tokens = tokens_str.split('-')
        ipa_output = []
        position = 0
        
        for t in tokens:
            t = t.strip()
            # Token is not a number → skip
            if not t.isdigit():
                continue
            
            tid = int(t)
            if tid not in self.id_to_ipa:
                continue
            
            # Add IPA char with position
            ipa_output.append({self.id_to_ipa[tid]: position})
            position += 1
        
        return ipa_output
    
    def clear_text(self, text:str):
        """
        Clear text by removing unwanted characters
        """
        allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ ")
        return ''.join(c for c in text if c in allowed_chars)
    
    def get_ipa_confidence(self, text_input:str, audio_array, sample_rate=16000):
        """
        Main function to get IPA confidence score
        Args:
            text_input: Text
            audio_array: Audio data (numpy array)
            sample_rate: Audio sample rate
        Returns:
            Dict containing score, ipa, detail_scores
        """
        
        # ====== 1. Convert text to IPA ======
        correct_ipa = ipa.convert(self.clear_text(text_input))
        correct_ipa_tokens = [{t: i} for i, t in enumerate(correct_ipa)]
        
        # ====== 2. Load and predict audio with Whisper ======
        speech_array = np.array(audio_array)
        speech_array = torch.from_numpy(speech_array).float()
        speech_array = speech_array.squeeze()
        
        # Resample if needed
        if sample_rate != 16000:
            resampler = torchaudio.transforms.Resample(sample_rate, 16000)
            speech_array = resampler(speech_array)
        
        # Convert to numpy for processor
        audio_np = speech_array.numpy() if isinstance(speech_array, torch.Tensor) else speech_array
        
        # Whisper input features
        input_features = self.processor(
            audio_np, 
            sampling_rate=16000, 
            return_tensors="pt"
        ).input_features.to(self.device)
        
        # Generate with scores
        with torch.no_grad():
            outputs = self.model.generate(
                input_features,
                output_scores=True,
                return_dict_in_generate=True
            )
        
        # Decode predicted tokens
        pred_ids = outputs.sequences
        transcription = self.processor.batch_decode(pred_ids, skip_special_tokens=True)
        pred_tokens_str = transcription[0]
        
        # Convert predicted tokens to IPA format
        pred_tokens = self.convert_whisper_tokens_to_ipa(pred_tokens_str)
        
        print(f"Correct IPA: {correct_ipa_tokens}\n")
        print(f"Predicted tokens: {pred_tokens}\n")
       
       
        # ====== Best match alignment ======
        alignment_result = self.best_alignment_analysts(
            correct_ipa_tokens, 
            pred_tokens
        )
        
        # Handle cannot align case
        if alignment_result is None or not alignment_result.get('can_align', False):
            ipa_li = list(correct_ipa)
            detail_scores = [{char: 0.0} for char in ipa_li]
            return {
                'message': 'Cannot align - pronunciation too different',
                'score': 0.0,
                'ipa': correct_ipa,
                'detail_scores': detail_scores
            }
        
        # ====== 4. Calculate confidence based on alignment and Whisper scores ======
        results = []
        scores = outputs.scores  # List of tensors
        for alignment_info in alignment_result['alignment']:
            correct_idx = alignment_info['correct_index']
            correct_char = alignment_info['correct_char']
            pred_idx = alignment_info.get('pred_index')
            is_match = alignment_info['is_match']
            pred_char = alignment_info.get('pred_char', '')
            
            if is_match == "NOT EXIST" or pred_idx is None:
                # Not in prediction
                results.append({
                    'char': correct_char,
                    'confidence': 0.0,
                    'matched': False,
                    'predicted': None
                })
                print(f"{correct_char:>5s} → NOT EXIST (confidence: 0.00%)")
                
            elif is_match == "TRUE":
                # Match - Calculate confidence from scores
                if pred_idx < len(scores):
                    # Get probability distribution at this position
                    token_scores = scores[pred_idx][0]  # Shape: (vocab_size,)
                    probs = torch.nn.functional.softmax(token_scores, dim=-1)
                    
                    # Encode predicted char to get token ID
                    token_ids = self.processor.tokenizer.encode(f'-{self.ipa_to_id[pred_char]}', add_special_tokens=False)
                    
                    if len(token_ids) == 0:
                        conf = 0.0
                    else:
                        token_id = token_ids[0]
                        conf = probs[token_id].item() * 100
                else:
                    conf = 0.0
                
                results.append({
                    'char': correct_char,
                    'confidence': conf,
                    'matched': True,
                    'predicted': pred_char
                })
                print(f"{correct_char:>5s} → MATCH ({pred_char}) (confidence: {conf:.2f}%)")
                
            else:  # is_match == "FALSE"
                # Mismatch - get confidence of correct char in this position
                if pred_idx < len(scores):
                    token_scores = scores[pred_idx][0]
                    probs = torch.nn.functional.softmax(token_scores, dim=-1)
                    
                    # Encode correct char to get token ID
                    token_ids = self.processor.tokenizer.encode(f'-{self.ipa_to_id[correct_char]}', add_special_tokens=False)
                    
                    if len(token_ids) == 0:
                        conf = 0.0
                    else:
                        token_id = token_ids[0]
                        conf = probs[token_id].item() * 100
                else:
                    conf = 0.0
                
                results.append({
                    'char': correct_char,
                    'confidence': conf,
                    'matched': False,
                    'predicted': pred_char
                })
                print(f"{correct_char:>5s} → MISMATCH ({pred_char}) (confidence: {conf:.2f}%)")
        
        # ====== 5. Final result ======
        
        detail_scores = []
        total = 0.0
        count = 0
        for result in results:
            if result['char'] == ' ':
                # Special char - no score
                detail_scores.append({result['char']: 100.0})
            else:
                conf = result['confidence']
                detail_scores.append({result['char']: conf})
                total += conf
                count += 1
 
        score = total / count if count > 0 else 0.0
        
        return {
            'message': 'Success',
            'score': score,
            'ipa': correct_ipa,
            'detail_scores': detail_scores
        }
        
    @staticmethod
    def dtw_alignment(correct_tokens, pred_tokens):
        """
        Align using Dynamic Time Warping
        Returns alignment for each correct token
        """
        correct_chars = [list(t.keys())[0] for t in correct_tokens]
        pred_chars = [list(t.keys())[0] for t in pred_tokens]
        
        n, m = len(correct_chars), len(pred_chars)
        
        # DTW distance matrix
        dtw = [[float('inf')] * (m + 1) for _ in range(n + 1)]
        dtw[0][0] = 0
        
        # Fill DTW matrix
        for i in range(1, n + 1):
            for j in range(1, m + 1):
                cost = 0 if correct_chars[i-1] == pred_chars[j-1] else 1
                dtw[i][j] = cost + min(
                    dtw[i-1][j],      # deletion
                    dtw[i][j-1],      # insertion
                    dtw[i-1][j-1]     # substitution
                )
        
        # Backtrack to find alignment path
        alignment = []
        i, j = n, m
        
        while i > 0:
            if j == 0:
                # Predicted sequence ended, remaining correct chars are NOT EXIST
                alignment.append({
                    'correct_index': i-1,
                    'correct_char': correct_chars[i-1],
                    'pred_index': None,
                    'pred_char': '',
                    'is_match': 'NOT EXIST'
                })
                i -= 1
                continue
            
            # Find best path
            candidates = []
            if i > 0 and j > 0:
                candidates.append((dtw[i-1][j-1], 'match', i-1, j-1))
            if i > 0:
                candidates.append((dtw[i-1][j], 'delete', i-1, j))
            if j > 0:
                candidates.append((dtw[i][j-1], 'insert', i, j-1))
            
            min_cost, move, new_i, new_j = min(candidates)
            
            if move == 'match':
                # Match or substitution
                is_match = 'TRUE' if correct_chars[i-1] == pred_chars[j-1] else 'FALSE'
                alignment.append({
                    'correct_index': i-1,
                    'correct_char': correct_chars[i-1],
                    'pred_index': j-1,
                    'pred_char': pred_chars[j-1],
                    'is_match': is_match
                })
                i, j = new_i, new_j
            elif move == 'delete':
                # Correct char not in prediction
                alignment.append({
                    'correct_index': i-1,
                    'correct_char': correct_chars[i-1],
                    'pred_index': None,
                    'pred_char': '',
                    'is_match': 'NOT EXIST'
                })
                i = new_i
            else:  # insert
                # Extra char in prediction (skip it)
                j = new_j
        
        alignment.reverse()
        
        # Calculate alignment quality
        matches = sum(1 for a in alignment if a['is_match'] == 'TRUE')
        can_align = matches / len(correct_chars) >= 0.3  # At least 30% match
        
        return {
            'can_align': can_align,
            'explain': f"DTW alignment: {matches}/{len(correct_chars)} matched",
            'alignment': alignment
        }
    # {{"correct_index": 0, "correct_char": "h",  "pred_index": 0, "pred_char": "h",  "is_match": "TRUE"}},
    # "can_align": true/false,
    # "explain": "A summary describing how well the user's pronunciation matches and any IPA deviations.",
    # "alignment": [
    #     {{"correct_index": 0, "correct_char": "h",  "pred_index": 0, "pred_char": "h",  "is_match": "TRUE"}},
    #     {{"correct_index": 1, "correct_char": "ɛ",  "pred_index": 1, "pred_char": "ɛ",  "is_match": "TRUE"}},
    #     ...
    # ]

    def best_alignment_analysts(self, correct_tokens: list, pred_tokens: list):
        
        seq1 = "".join([next(iter(d)) for d in correct_tokens])
        seq2 = "".join([next(iter(d)) for d in pred_tokens])

        aligner = Align.PairwiseAligner()
        aligner.mode = 'global'
        aligner.match_score = 2
        aligner.mismatch_score = -1
        aligner.open_gap_score = -2
        aligner.extend_gap_score = -2

        alignments = aligner.align(seq1, seq2)
        best_alignment = alignments[0]

        formatted_alignment = []
        total_match_token = 0

        aligned_seq1, aligned_seq2 = best_alignment

        idx1, idx2 = 0, 0
        for char1, char2 in zip(aligned_seq1, aligned_seq2):
            if char1 == char2:
                formatted_alignment.append({
                    "correct_index": idx1,
                    "correct_char": char1,
                    "pred_index": pred_tokens[idx2][next(iter(pred_tokens[idx2]))],
                    "pred_char": char2,
                    "is_match": "TRUE"
                })
                total_match_token += 1
                idx1 += 1
                idx2 += 1
            elif char1 == '-':
                idx2 += 1
            elif char2 == '-':
                formatted_alignment.append({
                    "correct_index": idx1,
                    "correct_char": char1,
                    "is_match": "NOT EXIST"
                })
                idx1 += 1
            else: 
                formatted_alignment.append({
                    "correct_index": idx1,
                    "correct_char": char1,
                    "pred_index": pred_tokens[idx2][next(iter(pred_tokens[idx2]))],
                    "pred_char": char2,
                    "is_match": "FALSE"
                })
                idx1 += 1
                idx2 += 1

        res = {
            "alignment": formatted_alignment,
            "can_align": total_match_token > len(correct_tokens) / 5
        }
        return res

    def llm_alignment_analysis(self, correct_tokens:dict, pred_tokens:dict, client):
        """
        Use LLM to analyze alignment between correct IPA and predicted IPA
        """
        
        # Convert tokens to string representation
        correct_str = ''.join([list(t.keys())[0] for t in correct_tokens])
        pred_str = ''.join([list(t.keys())[0] for t in pred_tokens])
        if correct_str == pred_str:
            print("Exact match between correct and predicted IPA.")
            return {
                "can_align": True,
                "explain": "Exact match between correct and predicted IPA.",
                "alignment": [
                    {
                        "correct_index": i,
                        "correct_char": list(t.keys())[0],
                        "pred_index": i,
                        "pred_char": list(t.keys())[0],
                        "is_match": "TRUE"
                    } for i, t in enumerate(correct_tokens)
                ]
            }
        
        try:
            response = client.models.generate_content(
                model="gemini-2.0-flash",
                contents=self.prompt(correct_tokens, pred_tokens)
            )
            
            # Parse JSON response
            response_text = response.text.strip()
            # Remove markdown code blocks if present
            if response_text.startswith('```'):
                response_text = response_text.split('```')[1]
                if response_text.startswith('json'):
                    response_text = response_text[4:]
            
            result = json.loads(response_text)
            print(result)
            # Validate
            if not result.get('can_align', False):
                print(f"Cannot align: {result.get('explain', 'Unknown reason')}")
                return None
            
            # Validate alignment length
            if len(result['alignment']) != len(correct_tokens):
                print(f"Warning: LLM returned {len(result['alignment'])} alignments, expected {len(correct_tokens)}")
                return None
            
            return result
            
        except Exception as e:
            print(f"Error in LLM alignment: {e}")
            if hasattr(response, 'text'):
                print(f"Response: {response.text[:500]}")
            return None
        
    def prompt(self, correct_tokens:dict, pred_tokens:dict):
        correct_str = ''.join([list(t.keys())[0] for t in correct_tokens])
        pred_str = ''.join([list(t.keys())[0] for t in pred_tokens])
        return   f"""You are an expert in evaluating English pronunciation.  
        Your task is to compare the **Correct IPA** with the **Predicted IPA from an AI model**, then return the result **strictly in JSON** with detailed information for each IPA unit.

        Fields required:
        - correct_index: index of the IPA unit in the correct IPA sequence  
        - correct_char: IPA unit from the correct sequence  
        - pred_index: index of the corresponding IPA unit in the predicted sequence (may differ if the user speaks additional sounds)  
        - pred_char: IPA unit found in the predicted output (or "" if not present)  
        - is_match: "TRUE" if matched, "FALSE" if mismatched, "NOT EXIST" if the correct unit does not appear in the prediction  

        Evaluation Principles:
        1. Comparison must be **logical**, not position-locked: if the user speaks longer or adds sounds at the beginning/end, you must still check whether the correct IPA unit appears anywhere in the predicted sequence.  
        2. If the predicted IPA unit matches the correct one → TRUE  
        3. If the predicted unit exists but is different → FALSE  
        4. If the correct IPA unit does not appear at all → NOT EXIST  
        5. Predicted IPA may be **shorter or longer** than correct:
            - Longer prediction: ignore extra units, only evaluate the units in the correct IPA.  
            - Shorter prediction: units that do not appear → NOT EXIST.
        6. **can_align**:
        - TRUE if the predicted IPA can be reasonably aligned to the correct IPA overall, 
            even if some IPA units are missing, extra, or mismatched.
        - FALSE only if the predicted IPA is too different to align at all 
            (e.g. most core consonants/vowels do not match and it sounds like a different word).
        - Missing IPA units alone MUST NOT make can_align = FALSE.
        7. Output **JSON only** — no explanations outside the JSON.  
        8. Consider diphthongs: if correct has ['o', 'ʊ'] and predicted has 'oʊ' as one unit, both should match to the same pred_index.
        9. Always return full alignment for all correct IPA units.
        Example:
            Ex1:
            correct IPA: {{'h':0,'ɛ':1,'l':2,'o':3,'ʊ':4}}
            predict IPA: {{'h':0,'i':1,'l':2,'o':3,'ʊ':4}}
            -> At position 1 is_match = "FALSE"
            Ex2: 
            correct IPA: {{h:0, ɛ:1, ˈ:2, l:3, o:4, ʊ:5, h:6, a:7 ʊ:8, ə:9, r:10, j:11, u:12, ə:13, r:14, j:15, u:16, g:17, ʊ:18, d:19}}
            predict IPA: {{h:0, ɛ:1, ˈ:2, l:3, o:4, ʊ:5, h:6, a:7 ʊ:8, ə:9, r:10, j:11, u:12}}
            - > at 13,14,15,16,17,18  is_match = "NOT EXIST"
            Ex3
            correct IPA: {{h:0, ɛ:1, ˈ:2, l:3, o:4, ʊ:5, h:6, a:7 ʊ:8, ə:9, r:10, j:11, u:12 }}
            predict IPA: {{h:0, ɛ:1, ˈ:2, l:3, o:4, ʊ:5, h:6, a:7 ʊ:8, ə:9, r:10, ə:11, r:12, j:13, u:14}}
            - > Choose 1 any ər to align
        Input:
        - Correct IPA: {correct_tokens} → string: "{correct_str}"
        - Predicted IPA: {pred_tokens} → string: "{pred_str}"

        Expected JSON output structure:
        {{
            "can_align": true/false,
            "explain": "A summary describing how well the user's pronunciation matches and any IPA deviations.",
            "alignment": [
                {{"correct_index": 0, "correct_char": "h",  "pred_index": 0, "pred_char": "h",  "is_match": "TRUE"}},
                {{"correct_index": 1, "correct_char": "ɛ",  "pred_index": 1, "pred_char": "ɛ",  "is_match": "TRUE"}},
                ...
            ]
        }}

        CRITICAL: Return ONLY valid JSON, no markdown, no backticks, no explanation outside JSON.
        """