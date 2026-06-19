-- ============================================================
-- Script d'initialisation de la base de données restaurant
-- ============================================================

-- Créer la base de données (à exécuter en tant que superuser)
-- CREATE DATABASE restaurant_db;
-- \c restaurant_db;

-- Extension pour UUID (facultatif mais recommandé)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Nettoyage des anciennes tables si existantes
DROP VIEW IF EXISTS vue_stock_alerte CASCADE;
DROP VIEW IF EXISTS vue_articles_populaires CASCADE;
DROP VIEW IF EXISTS vue_ventes_jour CASCADE;
DROP TABLE IF EXISTS mouvements_stock CASCADE;
DROP TABLE IF EXISTS stocks CASCADE;
DROP TABLE IF EXISTS paiements CASCADE;
DROP TABLE IF EXISTS commande_items CASCADE;
DROP TABLE IF EXISTS commandes CASCADE;
DROP TABLE IF EXISTS tables_restaurant CASCADE;
DROP TABLE IF EXISTS menu_items CASCADE;
DROP TABLE IF EXISTS categories CASCADE;

-- ============================================================
-- TABLE : catégories de menu
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          SERIAL PRIMARY KEY,
    nom         VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : articles du menu (Prix en F CFA)
-- ============================================================
CREATE TABLE IF NOT EXISTS menu_items (
    id           SERIAL PRIMARY KEY,
    categorie_id INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    nom          VARCHAR(150) NOT NULL,
    description  TEXT,
    prix         DECIMAL(10,2) NOT NULL CHECK (prix >= 0), -- Prix en FCFA
    disponible   BOOLEAN DEFAULT TRUE,
    image_url    VARCHAR(500),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : tables du restaurant
-- ============================================================
CREATE TABLE IF NOT EXISTS tables_restaurant (
    id         SERIAL PRIMARY KEY,
    numero     INTEGER NOT NULL UNIQUE,
    capacite   INTEGER NOT NULL CHECK (capacite > 0),
    statut     VARCHAR(20) DEFAULT 'LIBRE' CHECK (statut IN ('LIBRE','OCCUPEE','RESERVEE')),
    zone       VARCHAR(50) DEFAULT 'SALLE', -- SALLE, TERRASSE, VIP / SALON
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : commandes
-- ============================================================
CREATE TABLE IF NOT EXISTS commandes (
    id           SERIAL PRIMARY KEY,
    table_id     INTEGER REFERENCES tables_restaurant(id) ON DELETE SET NULL,
    serveur      VARCHAR(100),
    statut       VARCHAR(20) DEFAULT 'EN_ATTENTE'
                     CHECK (statut IN ('EN_ATTENTE','EN_COURS','SERVIE','PAYEE','ANNULEE')),
    note         TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : lignes de commande
-- ============================================================
CREATE TABLE IF NOT EXISTS commande_items (
    id           SERIAL PRIMARY KEY,
    commande_id  INTEGER NOT NULL REFERENCES commandes(id) ON DELETE CASCADE,
    menu_item_id INTEGER NOT NULL REFERENCES menu_items(id),
    quantite     INTEGER NOT NULL DEFAULT 1 CHECK (quantite > 0),
    prix_unitaire DECIMAL(10,2) NOT NULL,
    note         TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : paiements
-- ============================================================
CREATE TABLE IF NOT EXISTS paiements (
    id           SERIAL PRIMARY KEY,
    commande_id  INTEGER NOT NULL REFERENCES commandes(id),
    montant      DECIMAL(10,2) NOT NULL CHECK (montant >= 0),
    methode      VARCHAR(30) DEFAULT 'ESPECES'
                     CHECK (methode IN ('ESPECES','CARTE','MOMO', 'OM', 'TICKET_RESTO')), -- Intégration MoMo/OM
    montant_recu DECIMAL(10,2),
    monnaie      DECIMAL(10,2),
    reference    VARCHAR(100), -- ID Transaction Orange Money / Mobile Money
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : stocks (Matières premières camerounaises)
-- ============================================================
CREATE TABLE IF NOT EXISTS stocks (
    id           SERIAL PRIMARY KEY,
    nom          VARCHAR(150) NOT NULL,
    categorie    VARCHAR(80),
    quantite     DECIMAL(10,3) NOT NULL DEFAULT 0,
    unite        VARCHAR(20) DEFAULT 'kg', -- kg, Litre, Sac, Régime, Carton
    seuil_alerte DECIMAL(10,3) DEFAULT 1,
    prix_unitaire DECIMAL(10,2) DEFAULT 0,
    fournisseur  VARCHAR(150),
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE : mouvements de stock
-- ============================================================
CREATE TABLE IF NOT EXISTS mouvements_stock (
    id         SERIAL PRIMARY KEY,
    stock_id   INTEGER NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    type_mouv  VARCHAR(20) CHECK (type_mouv IN ('ENTREE','SORTIE','AJUSTEMENT')),
    quantite   DECIMAL(10,3) NOT NULL,
    motif      TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 🇨🇲 DONNÉES DE SEED / INITIALISATION LOCALISÉE 🇨🇲
-- ============================================================

-- 1. Catégories locales
INSERT INTO categories (id, nom, description) VALUES
    (1, 'Entrées Tropicales',      'Petites douceurs, beignets et amuse-bouches traditionnels'),
    (2, 'Plats du Pays',           'Plats de résistance gourmands et mijotés d''Afrique centrale'),
    (3, 'Douceurs Sucrées',        'Desserts tropicaux raffinés aux saveurs équatoriales'),
    (4, 'Rafraîchissements',       'Boissons fraîches locales, jus de fruits pressés et bières nationales')
ON CONFLICT (id) DO NOTHING;

-- 2. Articles du menu (Prix réels en Francs CFA - XAF)
INSERT INTO menu_items (id, categorie_id, nom, description, prix) VALUES
    -- Entrées (Id 1)
    (1, 1, 'Samosas Croustillants au Bœuf (3pcs)',  'Petits triangles dorés farcis à la viande hachée épicée', 2000.00),
    (2, 1, 'Koki de Titi (Mini portion)',           'Gâteau de haricots broyés à l''huile de palme rouge et piment', 3000.00),
    (3, 1, 'Salade Tropicale Avocat-Mangue',        'Mélange fraîcheur d''avocat de Buea, mangues de Njombé et coriandre', 3000.00),
    (4, 1, 'Beignets Accra de Poisson',            'Bouchées de manioc et de morue croustillantes aux épices locales', 2500.00),

    -- Plats chauds d''exceptions (Id 2)
    (5, 2, 'Ndolè Impérial Royal',                  'Feuilles de Ndolè mijotées aromatiques, arachides fraîches, crevettes de Kribi, viandes de bœuf et miondo', 8500.00),
    (6, 2, 'Poulet DG de l''Haut-Nkam',              'Mijoté de poulet fermier aux plantains mûrs frits, poivrons colorés et carottes fondantes', 7000.00),
    (7, 2, 'Bar Entier Braisé au Charbon',          'Bar entier fumé au charbon de bois aux épices de saison, frites de plantain alloco', 7500.00),
    (8, 2, 'Eru Spécial de Manyu',                  'Mélange traditionnel de feuilles d''eru et koka, tripes, écrevisses marines, huile de palme et fufu', 6500.00),
    (9, 2, 'Taro Royal Sauce Jaune',                'Taro pilé à la texture parfaite, nappé de sauce jaune onctueuse et assortiments de tripes de bœuf', 8000.00),
    (10,2, 'Porc Kondré traditionnel',               'Ragoût de plantains non mûrs braisé au porc tendre et herbes paysannes', 6500.00),

    -- Desserts (Id 3)
    (11,3, 'Moelleux Choco-Piment Penja',           'Cœur fondant au chocolat noir aromatisé d une touche de poivre blanc de Penja', 2500.00),
    (12,3, 'Carpaccio de Papaye & Ananas Victoria', 'Tranches ultrafines de fruits de Mbouda infusées au sirop de citronnelle', 2000.00),
    (13,3, 'Beignets Banane au Caramel de Coco',    'Dégustation de beignets chauds croustillants à la banane et noix de coco caramélisée', 2500.00),

    -- Boissons authentiques (Id 4)
    (14,4, 'Bissap Classique (Hibiscus)',           'Infusion glacée de fleurs d''oseille rouge infusées à l''ananas et menthe', 1000.00),
    (15,4, 'Jus de Gingembre énergisant',           'Crush de racine de gingembre fraîche pressé à froid au citron vert', 1500.00),
    (16,4, 'Guinness Grande (65cl)',                'La célèbre stout nationale culte, servie glacée', 1800.00),
    (17,4, 'Bière Kadji / Beaufort Light (65cl)',    'Sélection SABC des bières blondes préférées du Cameroun', 1500.00),
    (18,4, 'Top Ananas / Djino Pamplemousse',        'Gamme iconique de sodas du Cameroun', 1000.00),
    (19,4, 'Eau minérale Tangui (1.5L)',            'Eau de source filtrée des monts du Cameroun', 1000.00)
ON CONFLICT (id) DO NOTHING;

-- 3. Tables Restaurant (Salle, Terrasse cosy, VIP / Boukarou)
INSERT INTO tables_restaurant (numero, capacite, zone) VALUES
    (1,  2, 'Salle'),
    (2,  2, 'Salle'),
    (3,  4, 'Salle'),
    (4,  4, 'Salle'),
    (5,  4, 'Salle'),
    (6,  6, 'Salle VIP'),
    (7,  6, 'Salle VIP'),
    (8,  8, 'Espace Banquets'),
    (9,  2, 'Terrasse Alloco Tree'),
    (10, 2, 'Terrasse Alloco Tree'),
    (11, 4, 'Terrasse Alloco Tree'),
    (12, 4, 'Terrasse Alloco Tree'),
    (13, 2, 'Bar Cacao'),
    (14, 2, 'Bar Cacao')
ON CONFLICT (numero) DO NOTHING;

-- 4. Stocks d''ingrédients typiques pour notre approvisionnement cuisine
INSERT INTO stocks (nom, categorie, quantite, unite, seuil_alerte, prix_unitaire, fournisseur) VALUES
    ('Feuilles de Ndolè triées',  'Légumes',            4.0,  'kg',      10.0,  1500.00, 'Marché de Bonabéri'),
    ('Plantains mûrs (Doigts)',   'Féculents',         250.0,  'unité',   50.0,   150.00, 'Coopérative de Mbouda'),
    ('Crevettes fraîches de mer', 'Poissons/Crustacés', 35.0,  'kg',       8.0,  6000.00, 'Débarcadère de Kribi'),
    ('Poivre Blanc de Penja',     'Épices',              5.0,  'kg',       1.0,  9000.00, 'GIE Penja Spices'),
    ('Huile de palme rouge',      'Épicerie',           60.0,  'Litre',   15.0,  1200.00, 'Pressoir de Manyu'),
    ('Bâtons de Manioc (Miondo)', 'Féculents',         500.0,  'unité',  100.0,   120.00, 'Producteurs d''Obala'),
    ('Feuilles d''Eru fraîches',   'Légumes',            30.0,  'kg',       8.0,  2200.00, 'Marché Mfoundi'),
    ('Fleurs de Bissap séchées',  'Épicerie',           15.0,  'kg',       3.0,  1500.00, 'Fournisseurs de Maroua'),
    ('Casiers de Guinness 65cl',  'Boissons',           10.0,  'casier',   2.0, 14000.00, 'SABC Douala'),
    ('Casiers Beaufort/Kadji',    'Boissons',           15.0,  'casier',   3.0, 12000.00, 'SABC Douala'),
    ('Viande de bœuf hachée',     'Viandes',            25.0,  'kg',       6.0,  3500.00, 'Boucherie Centrale'),
    ('Poissons Bar Entiers Gros', 'Poissons/Crustacés', 50.0,  'unité',   15.0,  2800.00, 'Pêcheries du Wouri'),
    ('Racines de Gingembre',      'Épicerie',           12.0,  'kg',       3.0,  1200.00, 'Marché de Foumban'),
    ('Chocolat Noir de couverture','Épicerie',          10.0,  'kg',       2.0,  5500.00, 'ChocoCam Douala')
ON CONFLICT DO NOTHING;

-- 5. Commandes de test (Simulation d''une table active et d''une table payée)
-- Commande payée pour l'historique
INSERT INTO commandes (id, table_id, serveur, statut, note) VALUES
    (1, 3, 'Francis', 'PAYEE', 'Client très satisfait du Ndolè')
ON CONFLICT (id) DO NOTHING;

INSERT INTO commande_items (commande_id, menu_item_id, quantite, prix_unitaire) VALUES
    (1, 5, 2, 8500.00), -- 2x Ndolè Impérial
    (1, 14, 2, 1000.00), -- 2x Bissap
    (1, 11, 1, 2500.00)  -- 1x Moelleux Choco-Piment
ON CONFLICT (id) DO NOTHING;

INSERT INTO paiements (commande_id, montant, methode, montant_recu, monnaie, reference) VALUES
    (1, 21500.00, 'MOMO', 21500.00, 0.00, 'MOMO-OM-38290130')
ON CONFLICT (id) DO NOTHING;

-- Commande en cours d'attente
INSERT INTO commandes (id, table_id, serveur, statut, note) VALUES
    (2, 6, 'Clarisse', 'EN_COURS', 'Sans piment dans le Poulet DG svp')
ON CONFLICT (id) DO NOTHING;

INSERT INTO commande_items (commande_id, menu_item_id, quantite, prix_unitaire) VALUES
    (2, 6, 2, 7000.00),  -- 2x Poulet DG
    (2, 16, 2, 1800.00)  -- 2x Guinness Grande
ON CONFLICT (id) DO NOTHING;


-- ============================================================
-- VUES UTILES POUR L'ADMINISTRATION CAFE / RESTAURANT
-- ============================================================

CREATE OR REPLACE VIEW vue_ventes_jour AS
SELECT
    DATE(p.created_at)  AS date_vente,
    COUNT(DISTINCT p.id) AS nb_transactions,
    COUNT(DISTINCT c.id) AS nb_commandes,
    SUM(p.montant)       AS total_ventes_fcfa,
    p.methode
FROM paiements p
JOIN commandes c ON c.id = p.commande_id
GROUP BY DATE(p.created_at), p.methode
ORDER BY date_vente DESC;

CREATE OR REPLACE VIEW vue_articles_populaires AS
SELECT
    m.nom,
    cat.nom AS categorie,
    SUM(ci.quantite)   AS total_vendus,
    SUM(ci.quantite * ci.prix_unitaire) AS chiffre_affaires_fcfa
FROM commande_items ci
JOIN menu_items m  ON m.id  = ci.menu_item_id
JOIN categories cat ON cat.id = m.categorie_id
GROUP BY m.id, m.nom, cat.nom
ORDER BY total_vendus DESC;

CREATE OR REPLACE VIEW vue_stock_alerte AS
SELECT
    s.id,
    s.nom,
    s.categorie,
    s.quantite,
    s.unite,
    s.seuil_alerte,
    s.fournisseur,
    s.updated_at
FROM stocks s
WHERE s.quantite <= s.seuil_alerte
ORDER BY s.nom;