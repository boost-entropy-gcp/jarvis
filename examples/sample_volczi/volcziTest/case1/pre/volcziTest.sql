CREATE SCHEMA IF NOT EXISTS `{{project}}.jarvis_sample_volczi`;
CREATE TABLE IF NOT EXISTS `{{project}}.jarvis_sample_volczi.car{{tablePostfix}}`
	(
		id INTEGER NOT NULL OPTIONS(description="The license plate of the car "),
		brand STRING OPTIONS(description="The brand of the car "),
		price INTEGER OPTIONS(description="The price of the car ")
	);

TRUNCATE TABLE `{{project}}.jarvis_sample_volczi.car{{tablePostfix}}`;

INSERT INTO `{{project}}.jarvis_sample_volczi.car{{tablePostfix}}`
(id, brand, price)
VALUES
(1, "BMW", 800);
