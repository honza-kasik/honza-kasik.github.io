---
layout: post
title: "Dešťovka v Home Assistantu: měření hladiny vody v nádrži"
date: 2026-06-13
categories: [home-assistant, esphome, destovka]
excerpt: "Tlakové čidlo, ESPHome, PoE a Home Assistant jako praktická třešnička na dortu instalace dešťové nádrže."
---

Když jsme dělali nádrž na dešťovku, počítal jsem rovnou s tím, že ji časem napojím do Home Assistantu. K nádrži jsem proto nechal dotáhnout síťový kabel cat7 S/FTP, abych nemusel řešit Wi-Fi někde venku, baterky ani samostatné napájení.

Samotné měření hladiny tak pro mě nebylo nouzové dodělávání, ale spíš třešnička na dortu celé instalace dešťovky. Nádrž fungovala i bez toho, ale pořád chyběla jedna praktická věc: vědět, kolik vody v ní opravdu je.

Hlavně před blížícím se deštěm.

Když má přijít větší srážka, je dobré vědět, jestli je v nádrži ještě dost volné kapacity, nebo jestli má smysl předem extra zalít zahradu a udělat v nádrži místo. Bez měření jsem musel ven a oddělat poklop. S měřením vidím stav rovnou v Home Assistantu a můžu se podle toho rozhodnout.

![Při instalaci](/assets/images/destovka-hladina/03-nadrz.jpg)
*Momentka při instalaci. Vypadá to celkem OK, ne?*

## Princip

Použil jsem ponorné tlakové čidlo pro měření hladiny vody. Čidlo je dole v nádrži a podle tlaku vodního sloupce dává analogové napětí. Čím víc vody nad čidlem, tím vyšší tlak a tím vyšší výstupní napětí.

Na AliExpressu se podobná čidla prodávají v několika variantách: 4–20 mA, 0–10 V, RS485, 0–5 V nebo 0–3.3 V. Pro ESP32 je zásadní použít variantu s výstupem vhodným pro jeho ADC vstup. Na ADC vstup ESP32 nesmí přijít víc než 3,3 V, jinak hrozí poškození desky.

Já tedy používám analogovou variantu vhodnou pro přímé čtení ESP32 a napětí čtu přes ADC vstup. Konkrétně jsem bral variantu s rozsahem `0–3 m`, kabelem `5 m`, výstupem `0–3.3 V` a napájením `5 V`. Rozsah `0–3 m` je pro moji nádrž vyšší než skutečná maximální hladina, ale pořád dává dostatečnou rezervu i použitelné rozlišení. To je pro ESP32 praktická kombinace: napájení `5 V` i ADC vstup jsou dostupné přímo na desce, takže není potřeba žádný další převodník.

Je dobré počítat i s tím, že v kabelu čidla je tenká hadička pro kompenzaci atmosférického tlaku. Čidlo tak neměří absolutní tlak proti uzavřenému prostoru v kabelu, ale tlak vodního sloupce vůči atmosféře. U mě konec kabelu vede do IP67 krabice s ESP32 a v tomhle zapojení to funguje bez problémů. Zkracování kabelu je kvůli hadičce dost nepraktické, protože nejde jen o pár vodičů v bužírce.

## Hardware

Základ je deska Olimex ESP32-POE. Hlavní důvod je jednoduchý: když už mám k nádrži dotažený Cat7 S/FTP kabel, dává smysl po něm řešit komunikaci i napájení. Jeden kabel, žádná Wi-Fi a žádný zdroje.

Zapojení čidla je jednoduché:

| ESP32-POE | Tlakové čidlo |
| --- | --- |
| `+5V` | `VCC` |
| `GND` | `GND` |
| `GPIO35` | `OUT` |

Mezi výstup čidla a ADC vstup ESP32 jsem dal sériový odpor `4.7 kΩ` a na vstup kondenzátor `100 µF` proti zemi. Ne proto, že bych to měl nějak dokonale spočítané jako referenční návrh, ale hlavně proto, že jsem zrovna jiné vhodné součástky neměl po ruce.

U měření hladiny vody mi to nijak nevadí. Hladina se nemění skokově po milisekundách, takže pomalejší a klidnější měření je spíš výhoda. Zbytek vyhlazení pak ještě řeší filtry přímo v ESPHome.

![ESP32-POE v instalační krabici](/assets/images/destovka-hladina/01-rozvadec.jpg)
*Zapojené ESP32 s kondenzátorem na signálu proti GND, který řeší napěťové špičky a rezistorem jako ochrana ESP32*

## ESPHome

Celou konfiguraci jsem dal na GitHub:

<https://github.com/honza-kasik/water-cistern>

Po přidání do Home Assistantu vzniknou hlavně tyto entity:

- `Napětí čidla`
- `Hladina vody`
- `Zaplnění nádrže`

První hodnota je filtrované napětí z čidla. To je užitečné hlavně při kalibraci a kontrole, jestli čidlo dává rozumné hodnoty.

Druhá hodnota je přepočtená výška vodního sloupce v metrech.

Třetí hodnota je už praktické zaplnění nádrže v procentech.

Konfigurace používá Ethernet a nativní ESPHome API, takže není potřeba MQTT. První nahrání firmware je potřeba udělat typicky přes USB, další aktualizace už můžou běžet přes Ethernet.

![Terminál s logem](/assets/images/destovka-hladina/02-terminal.jpg)
*Hurá, funguje to! Zatím jen v konvi s vodou. Výška hladiny sedí.*

Přidání do Home Assistant už byla otázka doslova dvou kliků. ESPHome integrace vše našla a automaticky přidala.

## Kalibrace

Kalibrace je nejdůležitější část celého měření. Nestačí jen připojit čidlo a věřit číslům.

V konfiguraci jsou tři hlavní hodnoty:

```yaml
v_empty: "0.142"
v_full: "2.50"
tank_height_m: "2.194"
```

## Použitý HW

* [Olimex ESP32-POE](https://www.olimex.com/Products/IoT/ESP32/ESP32-POE/open-source-hardware)
* [Ponorné čidlo](https://www.aliexpress.com/item/1005008549415700.html)
* IP67 krabice, DIN lišta a průchodky z Hornbachu

EDIT: Doplněny podrobnosti k použité variantě tlakového čidla a poznámka k hadičce v kabelu.
