from google import genai
import os
import eng_to_ipa as ipa
from transformers import AutoProcessor, AutoModelForCTC
import torch
import torchaudio
import numpy as np
import json
from google import genai
from app.schemas import *
from app.core import settings

class PronunciationService:
    def __init__(self):
        if not os.environ.get("GOOGLE_API_KEY"):
            os.environ["GOOGLE_API_KEY"] = settings.GOOGLE_API_KEY
        self.llm = genai.Client()
        self.processor = AutoProcessor.from_pretrained("vitouphy/wav2vec2-xls-r-300m-timit-phoneme")
        self.model = AutoModelForCTC.from_pretrained("vitouphy/wav2vec2-xls-r-300m-timit-phoneme")
    
        pass
    def get_ipa_confidence(self, text_correct, audio_array,sample_rate=16000):
    
        # ====== 1. Chuyển text đúng sang IPA ======
        ipa_correct = ipa.convert(text_correct).replace("ˈ", "").replace("ˌ", "")
        ipa_correct_tokens = list(ipa_correct)
        ipa_correct_tokens = [i for i in ipa_correct_tokens if i!=' ']
        
        # ====== 2. Load và predict audio ======
        speech_array, sampling_rate = np.array(audio_array), sample_rate
        speech_array = torch.from_numpy(speech_array).float()
        speech_array = speech_array.squeeze()
        
        if sampling_rate != 16000:
            resampler = torchaudio.transforms.Resample(sampling_rate, 16000)
            speech_array = resampler(speech_array)
        
        inputs = self.processor(speech_array, sampling_rate=16000, return_tensors="pt", padding=True)
        
        with torch.no_grad():
            logits = self.model(**inputs).logits
        
        probs = torch.nn.functional.softmax(logits, dim=-1)
        pred_ids = torch.argmax(probs, dim=-1)[0]
        pred_tokens_raw = self.processor.tokenizer.convert_ids_to_tokens(pred_ids)
        
        # ====== 3. Lọc predicted tokens (bỏ PAD, trùng lặp) ======
        pred_tokens = []
        pred_indices = []  # Lưu index gốc trong logits
        prev_token = ''
        
        for i, token in enumerate(pred_tokens_raw):
            if token in ['[PAD]', '', ' '] or token == prev_token:
                prev_token = token
                continue
            pred_tokens.append(token)
            pred_indices.append(i)
            prev_token = token
        
        ipa_predicted = ''.join(pred_tokens)
        print(f"Predicted IPA: {ipa_predicted}")
        print(f"Predicted tokens: {pred_tokens}\n")
        
        # ====== 4. Dùng LLM để phân tích alignment ======
        alignment_result = self.llm_alignment_analysis(
            ipa_correct_tokens, 
            pred_tokens,
            self.llm
        )
        
        if alignment_result is None:
            correct_ipa = ipa.convert(text_correct)
            ipa_li = list(correct_ipa)
            detail_scores = []
            total = 0.0

            if results is None:
                detail_scores = [{ipa: 0.0} for ipa in ipa_li]
                return {
                    'message': 'hello',
                    'score': 0.0,
                    'ipa': correct_ipa,
                    'detail_scores': detail_scores
                }
        # ====== 5. Tính confidence dựa trên LLM alignment ======
        results = []
        
        for i, correct_char in enumerate(ipa_correct_tokens):
            alignment_info = alignment_result['alignment'][i]
            pred_idx = alignment_info['pred_index']
            is_match_from_llm = alignment_info['is_match']  # LẤY TỪ LLM
            
            if pred_idx is None:
                # Không match
                results.append({
                    'char': correct_char,
                    'confidence': 0.0,
                    'matched': False,
                    'predicted': None
                })
                print(f"{correct_char:>5s} → NO MATCH (confidence: 0.00%)")
            else:
                # Có match/mismatch từ LLM
                pred_char = pred_tokens[pred_idx]
                logits_idx = pred_indices[pred_idx]
                
                # QUAN TRỌNG: Dùng is_match từ LLM để quyết định matched
                # Nếu LLM nói là match (diphthong case), thì matched = True
                if is_match_from_llm:
                    # Trường hợp diphthong: 'o' và 'ʊ' đều match với 'oʊ'
                    # Tính confidence cho ký tự combined 'oʊ'
                    combined_id = self.processor.tokenizer.encode(pred_char, add_special_tokens=False)
                    
                    if len(combined_id) == 0:
                        conf = 0.0
                    else:
                        combined_id = combined_id[0]
                        conf = probs[0, logits_idx, combined_id].item()
                    
                    matched = True
                    predicted_display = pred_char
                else:
                    # Trường hợp mismatch thực sự
                    correct_id = self.processor.tokenizer.encode(correct_char, add_special_tokens=False)
                    
                    if len(correct_id) == 0:
                        conf = 0.0
                    else:
                        correct_id = correct_id[0]
                        conf = probs[0, logits_idx, correct_id].item()
                    
                    matched = False
                    predicted_display = pred_char
                
                results.append({
                    'char': correct_char,
                    'confidence': conf * 100,
                    'matched': matched,
                    'predicted': predicted_display
                })
                
                status = "MATCH" if matched else f"✗ ({predicted_display})"
        
        # ====== 6. Summary ======
        avg_confidence = np.mean([r['confidence'] for r in results])
        match_rate = sum(r['matched'] for r in results) / len(results)
        
       
        # ====== 7. return result ====
                
        correct_ipa = ipa.convert(text_correct)
        ipa_li = list(correct_ipa)
        detail_scores = []
        total = 0.0

        if results is None:
            detail_scores = [{ipa: 0.0} for ipa in ipa_li]
            return {
                'message': 'hello',
                'score': 0.0,
                'ipa': correct_ipa,
                'detail_scores': detail_scores
            }
            
        i = 0
        for ipa_char in ipa_li:
            if ipa_char in [' ', "ˈ", 'ˌ']:
                detail_scores.append({ipa_char: 100})
            else:
                conf = results[i]['confidence']
                detail_scores.append({results[i]['char']: conf})
                total += conf
                i += 1

        score = total / i if i > 0 else 0.0

        return {
            'message': 'hello',
            'score': score,
            'ipa': correct_ipa,
            'detail_scores': detail_scores
        }
    
    def llm_alignment_analysis(self,correct_tokens, pred_tokens, client):
        
        correct_str = ''.join(correct_tokens)
        pred_str = ''.join(pred_tokens)
        
        prompt = f"""You are an expert in phonetic alignment (IPA - International Phonetic Alphabet).

TASK: Analyze whether two IPA sequences can be aligned, and if yes, provide the alignment mapping.

CORRECT IPA (ground truth): {correct_tokens} → as string: "{correct_str}"
PREDICTED IPA (from speech model): {pred_tokens} → as string: "{pred_str}"

IMPORTANT CONTEXT:
- The PREDICTED tokens are already split by the model (e.g., "oʊ" may appear as one token "oʊ" or split as ['o', 'ʊ'])
- The CORRECT tokens are split character-by-character from the IPA string
- **DIPHTHONGS/COMBINED SOUNDS**: If predicted has "oʊ" as ONE token but correct has ['o', 'ʊ'] as TWO tokens, this is CORRECT alignment:
* correct[i]='o' → pred[j]='oʊ' (is_match=TRUE, because 'o' is part of 'oʊ')
* correct[i+1]='ʊ' → pred[j]='oʊ' (is_match=TRUE, SAME pred_index, because 'ʊ' is also part of 'oʊ')
- Similar cases: "aɪ", "eɪ", "aʊ", "ɔɪ", "ɪə", etc. (diphthongs)
- **CRITICAL**: If multiple consecutive correct chars form a predicted token, they ALL map to the SAME pred_index with is_match=TRUE

PHONETIC MATCHING RULES:
- EXACT MATCH: Same character → is_match=TRUE
- DIPHTHONG MATCH: Correct char is part of predicted diphthong → is_match=TRUE
Example: correct='o', predicted='oʊ' → is_match=TRUE (not mismatch!)
- TRUE MISMATCH: Different phonemes that don't form a diphthong → is_match=FALSE
Example: correct='t', predicted='d' → is_match=FALSE

EXAMPLES:
Example 1: Diphthong case (ALL matches!)
Correct: ['h', 'ɛ', 'l', 'o', 'ʊ'] → "hɛloʊ"
Predicted: ['h', 'ɛ', 'l', 'oʊ'] → "hɛloʊ"
✓ Alignment:
    'h' → 'h' (index 0, is_match=TRUE)
    'ɛ' → 'ɛ' (index 1, is_match=TRUE)
    'l' → 'l' (index 2, is_match=TRUE)
    'o' → 'oʊ' (index 3, is_match=TRUE - 'o' is part of 'oʊ')
    'ʊ' → 'oʊ' (index 3, is_match=TRUE - 'ʊ' is part of 'oʊ', SAME index!)
→ This is 100% correct pronunciation! All is_match should be TRUE.

Example 2: True mismatch
Correct: ['h', 'ɛ', 'l', 'o', 'ʊ'] → "hɛloʊ"
Predicted: ['h', 'ɛ', 't', 'oʊ'] → "hɛtoʊ"
✓ Alignment:
    'h' → 'h' (index 0, is_match=TRUE)
    'ɛ' → 'ɛ' (index 1, is_match=TRUE)
    'l' → 't' (index 2, is_match=FALSE - different phonemes!)
    'o' → 'oʊ' (index 3, is_match=TRUE)
    'ʊ' → 'oʊ' (index 3, is_match=TRUE)

Example 3: True mismatch
Correct: ['h', 'a', 'ʊ', 'ə', 'r', 'j', 'u'] → "haʊərju"
Predicted: ['h', 'aʊ', 'aɪ', 'j', 'u'] → "haʊaɪju"
✓ Alignment:
    'h' → 'h' (index 0, is_match=TRUE)
    'a' → 'aʊ' (index 1, is_match=TRUE)
    'ʊ' → 'aʊ' (index 1, is_match=TRUE)
    'ə' → 'aɪ' (index 2, is_match=FALSE)
    'r' → 'ɪ' (index 2, is_match=FALSE)
    'j' → 'j' (index 3, is_match=TRUE)
    'u' → 'u' (index 4, is_match=TRUE)

Example 4: Different words. Pay attention here if they are different word.
Correct: ['t', 'ɛ', 'k', 'n', 'ɑ', 'l', 'ə', 'ʤ', 'i'] → "tɛknɑləʤi"
Predicted: ['h', 'ɛ', 'l', 'oʊ'] → "hɛloʊ"
✗ Too different → can_align=FALSE

INSTRUCTIONS:
1. Calculate similarity percentage between the TWO STRINGS (not just tokens)
2. Determine if alignment is possible:
- If similarity < 40% → can_align = FALSE
- If they are clearly different words → can_align = FALSE
- Otherwise → can_align = TRUE

3. If can_align = TRUE, provide CHARACTER-BY-CHARACTER alignment:
- Check if consecutive correct tokens can form a predicted token (diphthongs)
- If correct char(s) ARE PART OF the predicted token → is_match=TRUE
- Only mark is_match=FALSE for true phonetic mismatches

RESPONSE FORMAT (JSON only, no explanation):
{{
    "can_align": true/false,
    "similarity_percent": <0-100>,
    "reason": "<brief explanation if can_align=false>",
    "alignment": [
        {{"correct_index": 0, "correct_char": "h", "pred_index": 0, "pred_char": "h", "is_match": true}},
        {{"correct_index": 3, "correct_char": "o", "pred_index": 3, "pred_char": "oʊ", "is_match": true}},
        {{"correct_index": 4, "correct_char": "ʊ", "pred_index": 3, "pred_char": "oʊ", "is_match": true}},
        ...
    ]
}}

CRITICAL: 
- Return ONLY valid JSON, no markdown, no backticks
- alignment array must have exactly {len(correct_tokens)} elements (one for each correct character)
- pred_index must be within [0, {len(pred_tokens)-1}] or null
- is_match=TRUE if correct char is part of predicted token (diphthongs!)
- Check string similarity, not just token similarity!
"""
        try:
            response = client.models.generate_content(
                model="gemini-2.0-flash-exp",
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
            if not result['can_align']:
                return None
            
            # Validate alignment length
            if len(result['alignment']) != len(correct_tokens):
                print(f"Warning: LLM returned {len(result['alignment'])} alignments, expected {len(correct_tokens)}")
                return None
            
            return result
            
        except Exception as e:
            print(f"Error in LLM alignment: {e}")
            print(f"Response: {response.text[:500]}")
            return None

    