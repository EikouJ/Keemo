# ------------ Dockerfile corrigé ------------
FROM python:3.11-slim

# Installer les dépendances système minimales
RUN apt-get update && apt-get install -y build-essential && apt-get clean

# Définir le répertoire de travail
WORKDIR /app

# Copier les fichiers de dépendances
COPY requirements.txt .

# Installer les dépendances Python
RUN pip install --no-cache-dir -r requirements.txt

# Copier tout le contenu du dossier api-server dans /app
COPY . .

# Exposer le port d'écoute (utilisé par Render ou Docker)
EXPOSE 10000

# Lancer l'application FastAPI avec uvicorn
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "10000"]
