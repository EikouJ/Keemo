
---


````markdown
# 📚 N-gram FastAPI - Prédiction de mots

Ce projet est une API développée avec **FastAPI** permettant de prédire le mot suivant d'une phrase en se basant sur un modèle de n-grammes (bigrams, trigrams, etc.).

---

## 🚀 Fonctionnalités

- 🔤 Prédiction du prochain mot dans une langue personnalisée
- 📊 Utilise un modèle entraîné de n-grammes (trigram_model.pkl)
- ⚡ Interface API rapide avec FastAPI
- ✅ Requêtes simples via `GET`

---

## 🛠️ Installation

1. Clone ce dépôt :
```bash
git clone https://github.com/dave2024coding/ngram-api.git
cd ngram-api
````

2. Crée un environnement virtuel (optionnel mais recommandé) :

```bash
python -m venv env
source env/bin/activate  # sous Windows : env\Scripts\activate
```

3. Installe les dépendances :

```bash
pip install -r requirements.txt
```

---

## ▶️ Lancer le serveur FastAPI

```bash
uvicorn main:app --reload
```

L'API sera accessible à :
➡️ [https://ngram-api.onrender.com/](https://ngram-api.onrender.com/)

---

## 📡 Endpoint principal

### `GET /predict`

| Paramètre | Description                       | Exemple                                            |
| --------- | --------------------------------- | -------------------------------------------------- |
| `phrase`  | La phrase d'entrée                | `Bëlò bëkëë bëngakoan e bod bësë bëyënë ai mimbëṅ` |
| `top_k`   | (optionnel) Nombre de prédictions | `3`                                                |

📌 Exemple d’appel :

```
http://127.0.0.1:8000/predict?phrase=Bëlò bëkëë&top_k=3
```

---

## 📁 Structure du projet

```
ngram-api/
├── main.py               # Application FastAPI
├── trigram_model.pkl     # Modèle de probabilité
├── predict.py            # Fonctions de prédiction
├── requirements.txt      # Dépendances
└── README.md             # Description du projet
```

---

## ✍️ Auteur

Dave ([@dave2024coding](https://github.com/dave2024coding))

---

## 📜 Licence

Ce projet est libre. Tu peux le modifier et l'utiliser selon tes besoins.

```

---

```
