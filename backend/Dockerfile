# ═══════════════════════════════════════════════════════════════
# DevToolkit AI — Dockerfile multi-stage
# Stage 1 : Build Maven (cache des dépendances optimisé)
# Stage 2 : Runtime JRE slim (image finale ~180 MB vs ~600 MB)
# ═══════════════════════════════════════════════════════════════

# ── Stage 1 : Builder ────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copier pom.xml EN PREMIER pour profiter du cache Docker sur les dépendances.
# Si seul le code change, Maven ne re-télécharge pas les jars.
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Installation Maven (Alpine n'a pas mvn par défaut)
RUN apk add --no-cache maven

# Re-téléchargement propre avec Maven Alpine
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copier les sources et builder
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# Vérification que le jar existe
RUN ls -lh target/*.jar

# ── Stage 2 : Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Métadonnées
LABEL maintainer="DevToolkit AI Team"
LABEL description="DevToolkit AI — Spring Boot API with Spring AI"
LABEL version="1.0.0"

WORKDIR /app

# Utilisateur non-root pour la sécurité (bonne pratique prod)
RUN addgroup -S devtoolkit && adduser -S devtoolkit -G devtoolkit

# Copier uniquement le jar depuis le stage builder
COPY --from=builder /build/target/devtoolkit-api-0.0.1-SNAPSHOT.jar app.jar

# Donner la propriété à l'utilisateur non-root
RUN chown devtoolkit:devtoolkit app.jar
USER devtoolkit

# Port exposé (Render utilisera $PORT en override si besoin)
EXPOSE 8080

# Options JVM optimisées pour conteneur avec mémoire limitée (512 MB Render free)
# -XX:+UseContainerSupport  : détecte les limites mémoire du conteneur
# -XX:MaxRAMPercentage=75.0 : utilise 75% de la RAM dispo (laisse de la marge)
# -XX:+UseSerialGC          : GC léger pour petits conteneurs (< 1 GB RAM)
# -Djava.security.egd       : accélère le démarrage Spring Boot
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseSerialGC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
