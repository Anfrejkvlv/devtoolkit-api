# DevToolkit API

API REST d’outils pour développeurs, construite avec Spring Boot. DevToolkit regroupe des utilitaires de manipulation de code et de configuration, ainsi que des fonctions d’analyse assistées par IA.

## Fonctionnalités

- **Expressions régulières** : tester une expression régulière et en générer une à partir d’une description en langage naturel.
- **SQL** : formater, optimiser et expliquer des requêtes SQL.
- **YAML / JSON** : convertir YAML et JSON, et valider leur syntaxe.
- **JWT** : décoder un token JWT et obtenir une explication de ses claims.
- **Expressions Cron** : générer et expliquer des expressions Cron.
- **Configuration Spring** : générer et auditer des fichiers de configuration Spring.
- **IA** : intégration de modèles Google Gemini et de fournisseurs compatibles avec l’API OpenAI, notamment Groq.
- **Cache** : cache mémoire Caffeine en développement et Redis disponible pour la production.
- **Documentation** : documentation interactive OpenAPI / Swagger UI.

## Technologies

- Java 21
- Spring Boot 3.5.5
- Spring Web et WebFlux
- Spring Validation
- Spring AI
- Google Gemini et Groq
- Maven
- Caffeine / Redis
- Jackson YAML
- JJWT
- SQL Formatter
- Springdoc OpenAPI

## Prérequis

- Java 21 ou supérieur
- Maven 3.9 ou supérieur
- Une clé API Gemini pour les fonctions d’IA
- Optionnel : une clé Groq et une instance Redis pour la configuration de production

## Installation

Clonez le dépôt puis placez-vous dans son dossier :

```bash
git clone https://github.com/Anfrejkvlv/devtoolkit-api.git
cd devtoolkit-api
```

Compilez et lancez les tests :

```bash
./mvnw clean verify
```

Si le Maven Wrapper n’est pas disponible dans votre environnement, utilisez Maven directement :

```bash
mvn clean verify
```

## Configuration

La configuration est définie dans `src/main/resources/application.yml`. Les valeurs sensibles doivent être fournies via des variables d’environnement et ne doivent pas être commitées.

### Développement

Le serveur démarre sur le port `8081` par défaut :

```bash
export GEMINI_API_KEY="votre-cle-gemini"
export GEMINI_API_KEY1="votre-cle-gemini-secondaire" # optionnel
export GEMINI_API_KEY2="votre-cle-gemini-tertiaire"   # optionnel
export GROQ_API_KEY="votre-cle-groq"                  # optionnel
export FRONTEND_URL="http://localhost:4200"
export API_URL="http://localhost:8081"

mvn spring-boot:run
```

Le port peut être modifié avec `PORT` :

```bash
PORT=8080 mvn spring-boot:run
```

Par défaut, Redis est désactivé en développement et le cache mémoire est utilisé.

### Production

Le profil de production peut être activé avec :

```bash
export SPRING_PROFILES_ACTIVE=prod
```

Les variables suivantes sont notamment utilisées par `application-prod.yml` :

| Variable | Description |
| --- | --- |
| `GEMINI_API_KEY` | Clé API Gemini principale |
| `GROQ_API_KEY` | Clé API Groq |
| `REDIS_HOST` | Hôte Redis |
| `REDIS_PORT` | Port Redis |
| `REDIS_PASSWORD` | Mot de passe Redis |
| `FRONTEND_URL` | Origine autorisée pour le CORS |
| `API_URL` | URL publique de l’API |
| `PORT` | Port d’écoute du serveur |

En production, Redis est utilisé pour le cache et les connexions Redis sont configurées en TLS. Vérifiez que toutes les variables nécessaires sont définies avant le démarrage.

## Démarrage avec Maven

```bash
mvn spring-boot:run
```

L’application est alors accessible à l’adresse suivante :

```text
http://localhost:8081
```

Pour construire le fichier JAR :

```bash
mvn clean package
java -jar target/devtoolkit-api-0.0.1-SNAPSHOT.jar
```

## Documentation API

Une fois l’application démarrée, la documentation OpenAPI est disponible via :

- Swagger UI : `http://localhost:8081/swagger-ui/index.html`
- Spécification OpenAPI : `http://localhost:8081/v3/api-docs`

En production, remplacez le port et l’hôte par l’URL de votre déploiement.

## Endpoints disponibles

Tous les endpoints fonctionnels sont préfixés par `/api/v1`.

### Regex

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/regex/test` | Teste une expression régulière |
| `POST` | `/api/v1/regex/generate` | Génère une expression régulière avec l’IA |

Exemple :

```bash
curl -X POST http://localhost:8081/api/v1/regex/test \
  -H 'Content-Type: application/json' \
  -d '{"pattern":"\\d+","flags":"gm","input":"test 123 abc 456"}'
```

### SQL

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/sql/format` | Formate une requête SQL |
| `POST` | `/api/v1/sql/optimize` | Analyse et propose une optimisation |
| `POST` | `/api/v1/sql/explain` | Explique une requête en langage naturel |

Exemple :

```bash
curl -X POST http://localhost:8081/api/v1/sql/format \
  -H 'Content-Type: application/json' \
  -d '{"sql":"SELECT id,name FROM users WHERE active = true","dialect":"postgresql"}'
```

### YAML et JSON

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/yaml/to-yaml` | Convertit du JSON en YAML |
| `POST` | `/api/v1/yaml/to-json` | Convertit du YAML en JSON |
| `POST` | `/api/v1/yaml/validate` | Valide un document YAML ou JSON |

Exemple :

```bash
curl -X POST http://localhost:8081/api/v1/yaml/to-json \
  -H 'Content-Type: application/json' \
  -d '{"input":"name: devtoolkit\nversion: 1"}'
```

### JWT

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/jwt/decode` | Décode les parties d’un JWT |
| `POST` | `/api/v1/jwt/explain` | Explique le payload avec l’IA |

> **Sécurité :** le décodage d’un JWT ne vérifie pas nécessairement sa signature. N’utilisez pas les données décodées comme preuve d’authentification.

### Cron

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/cron/generate` | Génère une expression Cron depuis une description |
| `POST` | `/api/v1/cron/explain` | Explique une expression Cron |

### Propriétés Spring

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/spring-props/generate` | Génère une configuration Spring |
| `POST` | `/api/v1/spring-props/review` | Audite un fichier YAML Spring |

### État des modèles

| Méthode | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/models/status` | Retourne le modèle disponible et les modèles épuisés |

## Limites et bonnes pratiques

- Les requêtes envoyées aux fonctions d’IA sont limitées à 5 000 caractères.
- La limite configurée est de 60 requêtes par minute et par adresse IP.
- Ne transmettez jamais de secrets, mots de passe, tokens de production ou données personnelles aux endpoints d’analyse IA.
- Stockez les clés API uniquement dans des variables d’environnement ou un gestionnaire de secrets.
- Les réponses générées par l’IA doivent être vérifiées avant utilisation en production.

## Structure du projet

```text
src/
└── main/
    ├── java/com/devtoolkit/
    │   ├── config/       # Configuration Spring, IA, cache et Swagger
    │   ├── controller/   # Contrôleurs REST
    │   ├── exception/    # Exceptions applicatives
    │   ├── model/        # Modèles de réponse
    │   └── service/      # Logique métier
    └── resources/
        ├── application.yml
        └── application-prod.yml
```

## Licence

Le projet est documenté comme étant distribué sous licence [MIT](https://opensource.org/licenses/MIT).

## Auteur et contact

- Projet : [Anfrejkvlv/devtoolkit-api](https://github.com/Anfrejkvlv/devtoolkit-api)
- Site : [loubahasra-dev.netlify.app](https://loubahasra-dev.netlify.app)
- Contact : `loubahasra.dev@gmail.com`
