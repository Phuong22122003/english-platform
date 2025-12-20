from google import genai
import os
import eng_to_ipa as ipa
from transformers import WhisperProcessor, WhisperForConditionalGeneration
import torch
import torchaudio
import numpy as np
import json
from app.schemas import *
from app.core import settings
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(BASE_DIR, "model", "fineturning")

class PronunciationService:
    def __init__(self):
        if not os.environ.get("GOOGLE_API_KEY"):
            os.environ["GOOGLE_API_KEY"] = settings.GOOGLE_API_KEY
        self.llm = genai.Client()
        
        # Load Whisper model
        model_name = MODEL_PATH
        self.processor = WhisperProcessor.from_pretrained(model_name)
        self.model = WhisperForConditionalGeneration.from_pretrained(model_name)
        self.model.config.forced_decoder_ids = None
        self.model.generation_config.forced_decoder_ids = None
        self.model.generation_config.suppress_tokens = None
        self.model.config.suppress_tokens = None
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
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
        Convert Whisper output tokens (dạng "1-2-3-4") sang IPA format
        Output: list of dict [{ipa_char: position}, ...]
        """
        tokens = tokens_str.split('-')
        ipa_output = []
        position = 0
        
        for t in tokens:
            t = t.strip()
            # Token không phải số hoặc không hợp lệ → skip
            if not t.isdigit():
                continue
            
            tid = int(t)
            if tid not in self.id_to_ipa:
                continue
            
            # Thêm IPA char với position
            ipa_output.append({self.id_to_ipa[tid]: position})
            position += 1
        
        return ipa_output
    
    def get_ipa_confidence(self, text_correct:str, audio_array, sample_rate=16000):
        """
        Main function để đánh giá phát âm
        Args:
            text_correct: Text đúng cần phát âm
            audio_array: Audio data (numpy array)
            sample_rate: Sample rate của audio
        Returns:
            Dict chứa score, ipa, detail_scores
        """
        
        # ====== 1. Chuyển text đúng sang IPA ======
        ipa_correct = ipa.convert(text_correct.strip()).replace("ˈ", "").replace("ˌ", "")
        ipa_correct_tokens = [{t: i} for i, t in enumerate(ipa_correct)]
        # ====== 2. Load và predict audio với Whisper ======
        speech_array = np.array(audio_array)
        speech_array = torch.from_numpy(speech_array).float()
        speech_array = speech_array.squeeze()
        
        # Resample nếu cần
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
        
        # Generate với scores
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
        
        print(f"Correct IPA: {ipa_correct}")
        print(f"Predicted tokens: {pred_tokens}\n")
        
        # ====== 3. Dùng LLM để phân tích alignment ======
        alignment_result = self.llm_alignment_analysis(
            ipa_correct_tokens, 
            pred_tokens,
            self.llm
        )
        
        # Nếu không align được, trả về kết quả mặc định
        if alignment_result is None or not alignment_result.get('can_align', False):
            ipa_li = list(ipa_correct)
            detail_scores = [{char: 0.0} for char in ipa_li]
            return {
                'message': 'Cannot align - pronunciation too different',
                'score': 0.0,
                'ipa': ipa_correct,
                'detail_scores': detail_scores
            }
        
        # ====== 4. Tính confidence dựa trên LLM alignment và Whisper scores ======
        results = []
        scores = outputs.scores  # List of tensors, mỗi tensor shape: (batch_size, vocab_size)
        for alignment_info in alignment_result['alignment']:
            correct_idx = alignment_info['correct_index']
            correct_char = alignment_info['correct_char']
            pred_idx = alignment_info.get('pred_index')
            is_match = alignment_info['is_match']
            pred_char = alignment_info.get('pred_char', '')
            
            if is_match == "NOT EXIST" or pred_idx is None:
                # Không có trong prediction
                results.append({
                    'char': correct_char,
                    'confidence': 0.0,
                    'matched': False,
                    'predicted': None
                })
                print(f"{correct_char:>5s} → NOT EXIST (confidence: 0.00%)")
                
            elif is_match == "TRUE":
                # Match - lấy confidence từ scores
                if pred_idx < len(scores):
                    # Get probability distribution tại position này
                    token_scores = scores[pred_idx][0]  # Shape: (vocab_size,)
                    probs = torch.nn.functional.softmax(token_scores, dim=-1)
                    
                    # Encode predicted char để lấy token ID thật
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
                # Mismatch - lấy confidence của correct char (sẽ thấp vì model đã predict sai)
                if pred_idx < len(scores):
                    token_scores = scores[pred_idx][0]
                    probs = torch.nn.functional.softmax(token_scores, dim=-1)
                    
                    # Encode correct char để lấy token ID
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
        
        # ====== 5. Tính toán kết quả cuối cùng ======
        ipa_li = list(ipa.convert(text_correct))
        detail_scores = []
        total = 0.0
        count = 0
        result_idx = 0
        for ipa_char in ipa_li:
            if ipa_char in [' ', "ˈ", 'ˌ']:
                # Ký tự đặc biệt - không tính vào score
                detail_scores.append({ipa_char: 100.0})
            else:
                if result_idx < len(results):
                    conf = results[result_idx]['confidence']
                    detail_scores.append({results[result_idx]['char']: conf})
                    total += conf
                    count += 1
                    result_idx += 1
                else:
                    detail_scores.append({ipa_char: 0.0})
        
        score = total / count if count > 0 else 0.0
        
        return {
            'message': 'Success',
            'score': score,
            'ipa': ipa_correct,
            'detail_scores': detail_scores
        }
    
    def llm_alignment_analysis(self, correct_tokens, pred_tokens, client):
        """
        Sử dụng LLM để phân tích alignment giữa correct và predicted IPA
        """
        
        # Convert tokens to string representation
        correct_str = ''.join([list(t.keys())[0] for t in correct_tokens])
        pred_str = ''.join([list(t.keys())[0] for t in pred_tokens])
        
        prompt = f"""You are an expert in evaluating English pronunciation.  
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
6. **`can_align`**: TRUE if the predicted IPA is close enough to the correct IPA to conclude the user pronounced the intended word; FALSE if the pronunciation deviates too much or sounds like a different word.
7. Output **JSON only** — no explanations outside the JSON.  
8. Consider diphthongs: if correct has ['o', 'ʊ'] and predicted has 'oʊ' as one unit, both should match to the same pred_index.

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
        
        try:
            response = client.models.generate_content(
                model="gemini-2.0-flash",
                contents=prompt
            )
            
            # Parse JSON response
            response_text = response.text.strip()
            # Remove markdown code blocks if present
            if response_text.startswith('```'):
                response_text = response_text.split('```')[1]
                if response_text.startswith('json'):
                    response_text = response_text[4:]
            
            result = json.loads(response_text)
            
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