---
title: TapHome – moje největší chyba na stavbě
excerpt: Při stavbě rodinného domu se dá udělat spousta chyb a investovat do TapHome pro mě byla ta nejdražší chyba na celé stavbě.
tags:
- smarthome
- home-assistant
- tapome
---

Na začátku byla idea chytré domácnosti jak ji asi chce každý: spolehlivá, po drátu, s podporou a konfigurovatelná odkudkoliv. Potom co jsem vyhodil z výběru KNX kvůli nákladům a Loxone kvůli uzavřenosti (což byla chyba) jsem našel slovenský TapHome.

TapHome jsem vybral hlavně kvůli funkci packet parseru, která slibovala snadnou integraci všeho, co nemá oficiální integraci od TapHome a celý systém vyapdal díky tomu celkem otevřeně.

S odstupem času jsem se rozhodl sepsat proč jsem nakonec TapHome zahodil. Shrnuji své rok staré zkušenosti a některé věci nemusí být aktuální. Jedná se mé osobní názory v rámci recenze.

## První problém: dokumentace

Při [integraci Papouch Quido]({% post_url 2024-04-30-quido-taphome %}) jsem narazil na to, že v dokumentaci chybí podstatná část funkcí packet parseru. Musel jsme procházet existující, nezdokumentovaná řešení a podle názvů funkcí hádat, jestli by se mi třeba daná funkce nehodila.

Celá hierarchie zařízení nebyla (a možná stále není) v dokumentaci nikde zmíněna a musel jsem prostě hádat. Tehdy jsme to skousl s tím, že se třeba ukáže, že výhody převažují.

## Druhý problém: přenositelnost

TapHome má aplikaci. Jen aplikaci. Chcete cokoliv nakonfigurovat? Jedině s aplikací. Používáte platformu pro kterou není aplikace? Smůla. Rozchodil jsem horko těžko Windows aplikaci ve Wine a zbytek prostě programoval z mobilu s Adnroidem.

Přitom by stačilo jednoduché webové rozhraní. To zabalíte i do mobilní aplikace. Vždyť tak to dělá Home Assistant.

Opět jsem si sliboval, že se ukáže síla zařízení a třeba v to něco je.

## Třetí problém: doménově specifický jazyk

Pro programování čehokoliv používá TapHome svůj vlastní DSL. Proto je chabá dokumentace takový problém. Cykly jsou utrpení a musíte se přizpůsobit myšlení autora jazyka.

Co je ale ještě horší je to, že rozsah použitelných tokenů se liší v závislosti na kontextu. V packet parseru nemůžete použít vše co v automatizaci a naopak.

Třeba to ale znamená, že zařízení budou spolehlivá když už chybí dokumentace. No ne?

## Čtvrtý problém: nespolehlivost

Asi jsem tušil, co se bude dít, když jsem dával dohromady architekturu chytré domácnosti. Chtěl jsem stmívatelná světla. TapHome samozřejmě nabízí proprietární dimmery, ale chtěl jsem, aby v případě, že třeba TapHome zanikne, bylo vše rozšiřitelné a udržovatelné. Postavil jsem tedy světla nad DMX – profesionálním protokolem pro ovládání světel a aparatury na koncertech.

TapHome pro rotokol DMX nabízel bránu/gateway. Strávil jsem desítky hodin pokusy rozchodit ji, aby fungovala. Brána v impleemntaci firmware špatně provádela časování (příliš dlouhé pauzy) a dekodér to nezvládl. I jen jedno světlo připojené k dekodéru zuřivě blikalo. Vím, že podobné problémy měli i další uživatelé.

Tady jsem si užil první zkušenost s podporou.

## Pátý problém: neprofesionální podpora

Ticket systém? Nebo snad JIRA? Ale kdeže, klíčovým prvkem podpory je skupina na Facebooku, kde fungují zaměstanci TapHome. Když položíte dotaz, třeba budete mít štěstía nasměrují vás do soukromých zpráv/emailu.

S podporou jsem si i volal. Nakonec po cca měsíci dodali novou verzi firmware pro DMX bránu, která vše trochu stabilizovala. Problém ale přetrvával.

Jak tedy společnost profesionálně vše vyřešila? Stáhla bránu z nabídky, smazala VEŠKEROU dokumentaci a dokonce i zmínky z Facebookové skupiny. Prostě jakoby daný produkt neexistoval. Prý se jim podpora nevyplatí.

Po tomto incidentu jsme se rozhodl s TapHome skoncovat. Nechcete někdo DMX bránu? Zpátky ji nevezmou.

## Šestý problém: aktualizace

Při pohledu do skupiny na Facebooku je vidět, že častým problémem jsou aktualizace. S čím jsem měl problém já osobně bylo, že aktualizace nepopisovala změny, které jsou obsaženy. Běžné jsou ale problémy, kdy po aktualizaci firmware nastane problém.

Chcete přece aktualizovat by byl dům bezpečný, že? Tak tady prý podle "podpory" platí, že když vše funguje tak se nic neaktualizuje.

Vše jde ruku v ruce s nedostatečnou dokumentací.

## Závěr

Není vyloučeno, že si ještě něco vybavím. Přineslo mi to mnoho zkušeností a významnou finanční ztrátu.