-- Adds the pharmacy-wide settings table (Settings module: pharmacy info,
-- currency, tax, receipt template, units of measure, email/SMS config).
-- Singleton table -- only one row, id = 1.

CREATE TABLE settings (
    id                              INTEGER PRIMARY KEY DEFAULT 1,
    pharmacy_name                   VARCHAR(150) NOT NULL DEFAULT 'My Pharmacy',
    address                         VARCHAR(255),
    phone                           VARCHAR(30),
    email                           VARCHAR(150),
    currency_code                   VARCHAR(10) NOT NULL DEFAULT 'USD',
    currency_symbol                 VARCHAR(10) NOT NULL DEFAULT '$',
    default_tax_percent             NUMERIC(5,2) DEFAULT 0,
    receipt_header                  TEXT,
    receipt_footer                  TEXT,
    units_of_measure                VARCHAR(255) DEFAULT 'Tablet,Bottle,Box,Strip,Vial,Ampoule',
    email_notifications_enabled     BOOLEAN DEFAULT FALSE,
    smtp_host                       VARCHAR(150),
    smtp_port                       INTEGER,
    smtp_username                   VARCHAR(150),
    smtp_password                   VARCHAR(255),
    sms_notifications_enabled       BOOLEAN DEFAULT FALSE,
    sms_provider                    VARCHAR(50),
    sms_api_key                     VARCHAR(255),
    sms_sender_id                   VARCHAR(50),
    updated_at                      TIMESTAMP DEFAULT NOW(),
    CONSTRAINT settings_singleton CHECK (id = 1)
);

INSERT INTO settings (id) VALUES (1) ON CONFLICT (id) DO NOTHING;
