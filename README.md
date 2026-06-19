# 🍽️ Restaurant Manager — Application JavaFX + PostgreSQL

Application de gestion complète pour restaurant : menus, tables, commandes, paiements, stocks et rapports de ventes.

---

## 📋 Fonctionnalités

| Module | Fonctionnalités |
|--------|----------------|
| 🏠 **Tableau de bord** | Vue d'ensemble en temps réel : CA du jour, commandes actives, tables occupées, alertes stocks |
| 🍽️ **Menu** | Catégories et articles, prix, disponibilité |
| 🪑 **Tables** | Plan de salle visuel, statuts (Libre / Occupée / Réservée), zones (Salle, Terrasse, Bar) |
| 📋 **Commandes** | Création de commandes par table, ajout d'articles, suivi du statut (En attente → En cours → Servie → Payée) |
| 💳 **Paiements** | Encaissement (Espèces, Carte, Chèque, Ticket restaurant), calcul automatique de la monnaie, historique |
| 📦 **Stocks** | Inventaire, entrées/sorties, alertes de seuil minimum |
| 📊 **Rapports** | CA par jour/semaine/mois, top articles, répartition par mode de paiement |
| 🧾 **Factures** | Génération PDF automatique avec TVA, détail des articles, mode de paiement |

---

## ⚙️ Prérequis

| Outil | Version minimale | Vérification |
|-------|-----------------|--------------|
| Java JDK | 17 ou supérieur | `java -version` |
| Apache Maven | 3.8+ | `mvn -version` |
| PostgreSQL | 13+ | `psql --version` |

### Installation rapide des outils

**Ubuntu / Debian :**
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven postgresql postgresql-contrib
```

**macOS (Homebrew) :**
```bash
brew install openjdk@17 maven postgresql
brew services start postgresql
```

**Windows :**
- [Télécharger JDK 17](https://adoptium.net/)
- [Télécharger Maven](https://maven.apache.org/download.cgi)
- [Télécharger PostgreSQL](https://www.postgresql.org/download/windows/)

---

## 🗄️ Configuration de la base de données

### 1. Créer la base de données

```bash
# Se connecter à PostgreSQL
psql -U postgres

# Dans le prompt psql :
CREATE DATABASE restaurant_db;
\q
```

### 2. Exécuter le script d'initialisation

```bash
psql -U postgres -d restaurant_db -f sql/init.sql
```

Ce script crée toutes les tables, les vues et insère des données de démonstration (14 tables, 14 articles au menu, 16 références de stock).

### 3. Variables d'environnement (optionnel)

Vous pouvez pré-remplir la connexion via variables d'environnement :

```bash
export DB_URL="jdbc:postgresql://localhost:5432/restaurant_db"
export DB_USER="postgres"
export DB_PASSWORD="votre_mot_de_passe"
```

---

## 🚀 Lancer l'application

### Méthode 1 — Via Maven (recommandée, développement)

```bash
# Se placer dans le dossier du projet
cd restaurant-app

# Compiler et lancer
mvn javafx:run
```

### Méthode 2 — Compiler puis lancer le JAR

```bash
# Compiler (crée un fat JAR)
cd restaurant-app
mvn clean package -q

# Lancer
java -jar target/restaurant-manager-1.0.0.jar
```

### Méthode 3 — Script tout-en-un

```bash
cd restaurant-app
mvn clean package -q && java -jar target/restaurant-manager-1.0.0.jar
```

---

## 🖥️ Premier démarrage

1. Au lancement, une **fenêtre de connexion** s'affiche
2. Saisissez les paramètres de connexion PostgreSQL :
   - Hôte : `localhost`
   - Port : `5432`
   - Base : `restaurant_db`
   - Utilisateur : `postgres`
   - Mot de passe : votre mot de passe PostgreSQL
3. Cliquez sur **🔌 Tester la connexion** pour vérifier
4. Cliquez **Se connecter** pour ouvrir l'application

---

## 🧾 Génération de factures PDF

Les factures sont générées au format PDF et sauvegardées automatiquement sur le Bureau (`~/Desktop`).  
Pour les générer : onglet **💳 Paiements** → sélectionner un paiement → cliquer **🧾 Générer facture**.

---

## 📁 Structure du projet

```
restaurant-app/
├── pom.xml                          ← Configuration Maven
├── README.md                        ← Ce fichier
├── sql/
│   └── init.sql                     ← Script SQL d'initialisation
└── src/main/java/com/restaurant/
    ├── Main.java                    ← Point d'entrée JavaFX
    ├── config/
    │   └── DatabaseConfig.java      ← Gestion de la connexion JDBC
    ├── model/                       ← Entités métier
    │   ├── Category.java
    │   ├── MenuItem.java
    │   ├── RestaurantTable.java
    │   ├── Order.java
    │   ├── OrderItem.java
    │   ├── Payment.java
    │   └── Stock.java
    ├── dao/                         ← Accès aux données (JDBC)
    │   ├── MenuItemDAO.java
    │   ├── TableDAO.java
    │   ├── OrderDAO.java
    │   ├── PaymentDAO.java
    │   └── StockDAO.java
    ├── view/                        ← Interfaces JavaFX
    │   ├── DashboardView.java
    │   ├── MenuView.java
    │   ├── TableView.java
    │   ├── OrderView.java
    │   ├── PaymentView.java
    │   ├── StockView.java
    │   ├── SalesView.java
    │   └── MainView.java
    └── util/
        └── InvoiceGenerator.java    ← Génération PDF (iText)
```

---

## 🔧 Dépannage

### Erreur `java: command not found`
→ Installez JDK 17+ et ajoutez-le au PATH.

### Erreur `mvn: command not found`
→ Installez Maven et assurez-vous que `JAVA_HOME` est défini.

### Erreur de connexion PostgreSQL
→ Vérifiez que PostgreSQL est démarré :
```bash
# Linux
sudo systemctl start postgresql
# macOS
brew services start postgresql
```

### Erreur `FATAL: password authentication failed`
→ Vérifiez le mot de passe dans `pg_hba.conf` ou utilisez :
```bash
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'nouveau_mot_de_passe';"
```

### Fenêtre noire / JavaFX ne s'affiche pas (Linux)
→ Installez les bibliothèques graphiques :
```bash
sudo apt install -y libgtk-3-0 libgl1-mesa-glx
```

---

## 🛠️ Technologies utilisées

| Technologie | Rôle |
|------------|------|
| **Java 17** | Langage de programmation |
| **JavaFX 21** | Interface graphique |
| **PostgreSQL** | Base de données relationnelle |
| **JDBC (pgjdbc)** | Connecteur base de données |
| **iText 5** | Génération de factures PDF |
| **Maven** | Gestion de projet et dépendances |

---

## 📄 Licence

Projet libre — usage éducatif.
