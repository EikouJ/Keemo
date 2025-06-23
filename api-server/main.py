from fastapi import FastAPI
from pydantic import BaseModel
import re
from ngram_api.model.loader import load_ngram_model

app = FastAPI(title="API de Prédiction N-gram")

# Chargement du modèle
ngram_model_path = 'trigram_model.pkl'
n = 3
ngram_probabilities = load_ngram_model(ngram_model_path)

class PredictionRequest(BaseModel):
    phrase: str
    top_k: int = 3

def predict_next_word(phrase, n, ngram_probabilities, top_k=3):
    phrase = re.sub(r'[^\w\s]', '', phrase).lower()
    words = ['<s>'] * (n - 1) + phrase.split()
    prefix = tuple(words[-(n - 1):])

    if prefix in ngram_probabilities:
        return sorted(
            ngram_probabilities[prefix].items(),
            key=lambda item: item[1],
            reverse=True
        )[:top_k]
    return None

@app.post("/predict")
def get_prediction(request: PredictionRequest):
    result = predict_next_word(request.phrase, n, ngram_probabilities, request.top_k)
    if result:
        return {"predictions": [{"word": word, "probability": prob} for word, prob in result]}
    return {"message": "Aucune prédiction disponible pour la phrase donnée."}

@app.get("/health")
def health_check():
    return {"status": "ok"}
