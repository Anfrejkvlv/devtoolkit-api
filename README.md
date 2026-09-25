# DevToolkit API

**DevToolkit** est une API REST qui regroupe plusieurs outils utiles aux développeurs : manipulation de code et de configuration, analyse de requêtes SQL, traitement YAML/JSON, décodage de JWT et génération de configurations Spring.

Le projet intègre également des fonctionnalités assistées par IA afin de transformer certaines descriptions en langage naturel en résultats exploitables, par exemple générer une expression régulière, expliquer une requête SQL ou analyser une configuration Spring.

L'objectif est de réunir dans une même API des outils qui sont généralement utilisés séparément, tout en expérimentant une intégration de l'IA dans des tâches de développement quotidiennes.

## ✨ Fonctionnalités

### 🧩 Outils développeur

* Tester et générer des expressions régulières
* Formater, expliquer et analyser des requêtes SQL
* Convertir et valider YAML / JSON
* Décoder et expliquer des JWT
* Générer et expliquer des expressions Cron
* Générer et auditer des configurations Spring

### 🤖 Fonctionnalités IA

L'application peut utiliser différents fournisseurs de modèles :

* Google Gemini
* Fournisseurs compatibles avec l'API OpenAI, notamment Groq

L'IA intervient notamment pour :

* générer des expressions régulières à partir d'une description ;
* expliquer des requêtes SQL ;
* proposer des optimisations SQL ;
* expliquer les claims d'un JWT ;
* générer des expressions Cron ;
* générer des propriétés Spring ;
* auditer des configurations Spring.

Les réponses générées sont considérées comme des **propositions** et doivent être vérifiées avant leur utilisation dans un environnement de production.

## 🏗️ Architecture

Le projet suit une architecture Spring Boot classique avec une séparation entre contrôleurs REST, logique métier, configuration et gestion des erreurs.

```text
src/
└── main/
    ├── java/com/devtoolkit/
    │   ├── config/       # Configuration Spring, IA, cache et OpenAPI
    │   ├── controller/   # API REST
    │   ├── exception/    # Gestion des exceptions
    │   ├── model/        # Modèles de réponse
    │   └── service/      # Logique métier
    └── resources/
        ├── application.yml
        └── application-prod.yml
```

Les endpoints fonctionnels utilisent le préfixe :

```text
/api/v1
```

La documentation interactive est disponible via OpenAPI / Swagger UI.

## 🧠 Intégration IA

L'une des particularités du projet est la possibilité d'utiliser plusieurs fournisseurs de modèles.

L'application utilise **Spring AI** comme couche d'intégration avec les modèles.

Cela permet de garder l'intégration relativement indépendante du fournisseur utilisé par l'environnement.

```text
Application
     │
     ▼
   Spring AI
     │
     ├── Gemini
     │
     └── Groq / compatible OpenAI
```

L'IA est utilisée comme outil d'assistance et non comme source de vérité.

Par exemple, pour l'audit d'une configuration Spring :

```text
Configuration utilisateur
        ↓
      API
        ↓
     Spring AI
        ↓
Analyse / suggestions
        ↓
Résultat à vérifier
```

Cette approche permet notamment de garder une validation humaine avant l'utilisation d'une réponse générée.

## ⚡ Cache

Le projet utilise deux stratégies de cache selon l'environnement.

### Développement

Le cache mémoire **Caffeine** est utilisé pour garder une configuration simple et locale.

### Production

**Redis** peut être utilisé comme cache distribué.

```text
                 ┌──────────────┐
Request ────────►│ Application  │
                 └──────┬───────┘
                        │
               ┌────────┴────────┐
               │                 │
            Caffeine           Redis
             local           distributed
```

L'intérêt de cette séparation est de disposer d'un cache local simple en développement tout en permettant une solution partagée lorsque plusieurs instances de l'application sont utilisées.

Redis est configuré avec TLS dans le profil de production.

## 🔐 Sécurité et limites

Le projet applique plusieurs précautions autour des fonctionnalités d'analyse.

### JWT

Le endpoint de décodage d'un JWT **ne vérifie pas nécessairement sa signature**.

Décoder le contenu d'un token ne signifie donc pas que le token est authentique.

Il ne faut jamais utiliser les données simplement décodées comme preuve d'authentification.

### Données envoyées aux modèles

Les utilisateurs ne doivent pas transmettre :

* mots de passe ;
* tokens de production ;
* clés secrètes ;
* données personnelles sensibles ;
* autres informations confidentielles.

Les réponses générées par l'IA doivent également être vérifiées avant d'être utilisées dans un contexte de production.

### Limitation des requêtes

Les fonctionnalités IA imposent notamment :

* une taille maximale de **5 000 caractères** par requête ;
* une limite de **60 requêtes par minute et par adresse IP**.

Ces limites permettent de mieux contrôler l'utilisation des endpoints et les coûts associés aux appels aux modèles.

## 🛠️ Stack technique

| Domaine       | Technologies                   |
| ------------- | ------------------------------ |
| Langage       | Java 21                        |
| Backend       | Spring Boot 3.5.5              |
| Web           | Spring Web, Spring WebFlux     |
| Validation    | Spring Validation              |
| IA            | Spring AI, Gemini, Groq        |
| Cache         | Caffeine, Redis                |
| Parsing       | Jackson YAML                   |
| JWT           | JJWT                           |
| Documentation | Springdoc OpenAPI / Swagger UI |
| Build         | Maven                          |

## 🚀 Installation

### Prérequis

* Java 21 ou supérieur
* Maven 3.9 ou supérieur
* Une clé API Gemini pour les fonctionnalités IA
* Optionnel : clé Groq
* Optionnel : Redis pour le profil de production

### Cloner le projet

```bash
git clone https://github.com/Anfrejkvlv/devtoolkit-api.git
cd devtoolkit-api
```

### Vérifier le projet

```bash
./mvnw clean verify
```

Ou avec Maven installé localement :

```bash
mvn clean verify
```

## ⚙️ Configuration

La configuration principale se trouve dans :

```text
src/main/resources/application.yml
```

Les informations sensibles doivent rester en dehors du dépôt et être fournies via des variables d'environnement ou un mécanisme de gestion des secrets.

### Développement

Le serveur démarre par défaut sur le port `8081`.

```bash
export GEMINI_API_KEY="your-gemini-key"
export GEMINI_API_KEY1="your-second-gemini-key"
export GEMINI_API_KEY2="your-third-gemini-key"
export GROQ_API_KEY="your-groq-key"
export FRONTEND_URL="http://localhost:4200"
export API_URL="http://localhost:8081"

mvn spring-boot:run
```

Redis est désactivé par défaut en développement et le cache mémoire est utilisé.

Le port peut être modifié :

```bash
PORT=8080 mvn spring-boot:run
```

### Production

Activer le profil :

```bash
export SPRING_PROFILES_ACTIVE=prod
```

Les principales variables utilisées par `application-prod.yml` sont :

| Variable         | Rôle                           |
| ---------------- | ------------------------------ |
| `GEMINI_API_KEY` | Clé API Gemini                 |
| `GROQ_API_KEY`   | Clé API Groq                   |
| `REDIS_HOST`     | Hôte Redis                     |
| `REDIS_PORT`     | Port Redis                     |
| `REDIS_PASSWORD` | Mot de passe Redis             |
| `FRONTEND_URL`   | Origine autorisée pour le CORS |
| `API_URL`        | URL publique de l'API          |
| `PORT`           | Port d'écoute                  |

## ▶️ Démarrer l'application

```bash
mvn spring-boot:run
```

L'API est alors accessible à :

```text
http://localhost:8081
```

Pour créer le JAR :

```bash
mvn clean package
```

Puis :

```bash
java -jar target/devtoolkit-api-0.0.1-SNAPSHOT.jar
```

## 📚 Documentation API

Une fois l'application démarrée :

**Swagger UI**

```text
http://localhost:8081/swagger-ui/index.html
```

**OpenAPI**

```text
http://localhost:8081/v3/api-docs
```

## 🌐 API

### Regex

| Méthode | Endpoint                 | Description                      |
| ------- | ------------------------ | -------------------------------- |
| POST    | `/api/v1/regex/test`     | Tester une expression régulière  |
| POST    | `/api/v1/regex/generate` | Générer une expression avec l'IA |

Exemple :

```bash
curl -X POST http://localhost:8081/api/v1/regex/test \
  -H 'Content-Type: application/json' \
  -d '{"pattern":"\\d+","flags":"gm","input":"test 123 abc 456"}'
```

### SQL

| Méthode | Endpoint               | Description                              |
| ------- | ---------------------- | ---------------------------------------- |
| POST    | `/api/v1/sql/format`   | Formater une requête                     |
| POST    | `/api/v1/sql/optimize` | Analyser / proposer une optimisation     |
| POST    | `/api/v1/sql/explain`  | Expliquer une requête en langage naturel |

Exemple :

```bash
curl -X POST http://localhost:8081/api/v1/sql/format \
  -H 'Content-Type: application/json' \
  -d '{"sql":"SELECT id,name FROM users WHERE active = true","dialect":"postgresql"}'
```

### YAML / JSON

| Méthode | Endpoint                | Description               |
| ------- | ----------------------- | ------------------------- |
| POST    | `/api/v1/yaml/to-yaml`  | Convertir du JSON en YAML |
| POST    | `/api/v1/yaml/to-json`  | Convertir du YAML en JSON |
| POST    | `/api/v1/yaml/validate` | Valider un document       |

### JWT

| Méthode | Endpoint              | Description                    |
| ------- | --------------------- | ------------------------------ |
| POST    | `/api/v1/jwt/decode`  | Décoder les parties d'un JWT   |
| POST    | `/api/v1/jwt/explain` | Expliquer le payload avec l'IA |

> Le décodage d'un JWT ne constitue pas une vérification cryptographique de sa signature.

### Cron

| Méthode | Endpoint                | Description                   |
| ------- | ----------------------- | ----------------------------- |
| POST    | `/api/v1/cron/generate` | Générer une expression Cron   |
| POST    | `/api/v1/cron/explain`  | Expliquer une expression Cron |

### Configuration Spring

| Méthode | Endpoint                        | Description                      |
| ------- | ------------------------------- | -------------------------------- |
| POST    | `/api/v1/spring-props/generate` | Générer une configuration Spring |
| POST    | `/api/v1/spring-props/review`   | Auditer une configuration Spring |

### Modèles IA

```text
GET /api/models/status
```

Retourne l'état du modèle disponible et des modèles temporairement indisponibles ou épuisés.

## 🧪 Tests

La vérification complète du projet peut être lancée avec :

```bash
./mvnw clean verify
```

Cette étape permet de compiler le projet et d'exécuter la suite de tests configurée dans le repository.

## 🔎 Pourquoi ce projet ?

DevToolkit est principalement un projet d'exploration autour de deux idées :

1. Regrouper des outils de développement fréquemment utilisés derrière une API cohérente.
2. Expérimenter l'utilisation de l'IA dans des tâches de développement sans considérer les réponses générées comme automatiquement fiables.

Le projet permet ainsi d'explorer plusieurs problématiques qui se croisent dans une application réelle :

**API REST → intégration de modèles → sécurité → cache → gestion de configuration → validation → documentation.**

## 🚧 Pistes d'amélioration

Quelques évolutions naturelles du projet :

* ajouter davantage de tests d'intégration sur les endpoints IA ;
* améliorer l'observabilité des appels aux modèles ;
* mesurer les performances et le taux de hit du cache ;
* mieux gérer les quotas et erreurs des fournisseurs IA ;
* ajouter d'autres fournisseurs de modèles ;
* renforcer les mécanismes de validation des résultats générés ;
* enrichir les outils de configuration et d'analyse.

## 📄 Licence

Le projet est documenté comme étant distribué sous licence **MIT**.

## Auteur et contact

- Projet : [Anfrejkvlv/devtoolkit-api](https://github.com/Anfrejkvlv/devtoolkit-api)
- Site : [loubahasra-dev.netlify.app](https://loubahasra-dev.netlify.app)
- Contact : `eloubahasra@gmail.com`
