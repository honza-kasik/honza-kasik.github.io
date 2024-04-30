---
title: Integrace Papouch Quido do TapHome
excerpt: Potřebujete 100 digitálních vstupů a máte málo místa?
tags:
- smarthome
- taphome
---

Určitě už jste četli [zápisek o propojení Loxone s modulem Quido od Papoucha na vodnickém blogu](https://www.vodnici.net/2023/04/loxone-propojeni-s-modulem-quido-od-papoucha/). Protože jsem ale nakonec nešel cestou Loxone a zvolil pro systém chytré domácnosti TapHome, potřeboval jsem řešit stejný problém v jiném kontextu.

## Fyzické zapojení

Modul české výroby nabízí na šíři cca 12 běžných DIN modulů [60+40 digitálních vstupů](https://papouch.com/quido-eth-100-3-100-vstupu-3-vystupy-a-teplomer-p4641/) spolu s volitelným komunikačním rozhraním. Já vybral modul ETH s ethernetem.

Integrace s kabeláží chytré domácnosti je jednoduchá: do vypínače/tlačítka stačí přivést na jednom vodiči napětí a zbytek vodičů z kabelu můžou být vstupy. Pro běžný UTP kabel je to tedy 1+7 vstupů. Já ale nechal v každé místnosti minimálně pod jedním vypínačem pár vodičů pro připojení NTC teploměru.

V rozvaděči stačí pak svorku IGND Quida propojit s - pólem zdroje signálového napětí. Pro zachování galvanického oddělení vstupů je nutné použít jiný zdroj pro napájení Quida a jiný pro signály.

Datové propojení zajišťuje ethernetový kabel zapojený do switche v rozvaděči, který je samozřejmě propojen s routerem s DHCP.

Dokumentace Quida je skvělá. Opravdu by si z ní mnozí mohli brát příklad.

## Konfigurace Quida

Po připojení na IP adresu Quida v režimu WEB uživatele přivítá přehled vstupů a jejich stav. Vyzkoušel jsem si tu, že signalizace stavu vstupů funguje a přepnul jsem Quido do režimu UTP. V tomto režimu stačí nastavit cílovou IP adresu a port a Quido tam bude ve výchozím nastavení odesílat packet v protokolu Spinel po každé změně hodnoty jakéhokoliv vstupu.

UDP jsem zvolil proto, že poslání packetu bez TCP handshaku je mnohem rychlejší a zaručuje nižší odezvu.

Tím veškerá konfigurace Quida končí. Není to super?

## Packet parser — důvod, proč jsem zvolil Taphome

Ač má TapHome pár slabin (žádné web UI a podprůměrná dokumentace), u mě jasně vyhrál díky [packet parseru](https://taphome.com/CZ/support/453935111). Tato funkce dovoluje integraci *jakéhokoliv* rozumně napsaného API. Cloudového i lokálního. Binárního, v plaintextu, JSONu i XML. Můžete integrovat doslova cokoliv co připojíte k síti. V tomto je síla TapHome. Nemusíte se doprošovat integrace toho, či onoho a prostě si to uděláte sami. Tady zcela válcuje Loxone.

Dokumentace packet parseru pokulhává, ale zkusím sepsat, co jsem zjistil:

Nejvíce informací se dá načerpat z [oficiálního repozitáře s templates](https://github.com/taphome-official/packetparser_templates). Nic sice není zdokumentované, ale příkladů je tam spousta. Z [oficiální dokumentace](https://taphome.com/CZ/support/2335309829) se třeba nedozvíte k čemu přesně dobrý je listener skript.

V rámci struktury packet parseru máte možnost konfigurovat _modul_, který zastřešuje jedno nebo více zařízení. V mém případě konfiguruji UDP modul a u něj `localhost` hostname jako IP adresu a port na kterém očekávám UDP packety od Quida. Netuším, jestli vůbec jde v TapHome nakonfigurovat více síťových rozhraní a tak netuším, jaké využití konfigurace IP adresy má. Při nastavení `localhost` bude packet parser naslouchat na všech rozhraních a zároveň se tím vyhnu závislosti na pevné IP v síti.

U každého modulu lze v servisním nastavení nastavit:

* Listener script/skript posluchače, který zpracovává jakákoliv data, která v packet parseru přistanou v proměnné `RECEIVEDBYTES`
* Read script pro periodické čtení stavu
* Write script pro jednotlivý nebo periodický zápis hodnoty
* a dále také servisní atributy a servisní akce, které ale netuším k čemu slouží a nepátral jsem po tom

V detailech modulu lze nastavit proměnné, které chceme sdílet mezi skripty nebo modulem a zařízeními. Při konfiguraci proměnné je zde omezení v podobě typu proměnné, kde lze zvolit jen mezi číslem nebo řetězcem. Složitější struktury jako byte array tedy neuložíte. Nebo aspoň opět nevím jak na to.

V podstatě stejné věci jdou konfigurovat i u každého zařízení. Chápu to tak, že ve skriptech modulu lze provést složitější akci tak, aby se nemusela provádět u každého zařízení zvlášť.

## Spinel protokol a listener skript

[Spinel protokol](https://cdn.papouch.com/data/user-content/products/quido-standard-spolecne/quido-spinel_cz.pdf) je na první pohled proprietární zbytečnost na cestě za výsledkem. Na pohled druhý výrazně tuto cestu zkrátí.

Dobrou zprávou je, že ve výchozím stavu v UDP režimu posílá Quido stav všech vstupů při každé změně. Zpráva ve spinel protokolu vypadá nějak takto:

```python
['0b101010', '0b1100001', '0b0', '0b10010', '0b110001', '0b10011', '0b1101', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b11011', '0b10011001', '0b1011101', '0b1101']
```

Děsivé, že? Ale kdepak. Prvních (z pohledu TapHome jde o byte array) 8 bytů jsou stavové informace a protokol. Poslední 2 byty to samé. Nás zajímá 104 bitů mezi těmito skupinami.

```python
['0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b0', '0b11011', '0b10011001']
```

Lepší, že? Teď je tu takový malý trik. Ač TapHome používá indexaci v array klasicky zleva do prava, nejvýznamnější bit je úplně pravo a pozice bitu zprava odpovídá stavu vstupu. V příkladu nám tedy Quido říká, že zapnuté vstupy jsou 1, 4, 5, 8, 9, 10, 12 a 13.

Pokud chceme zjistit stav vstupu 1, naivní kód v TapHome skriptovacím jazyce vypadá takto:

```pascal
GETBIT(GETAT(RECEIVEDBYTES, LEGTH(RECEIVEDBYTES) - 3), 0)
```

Nejdříve se podíváme na poslední bajt se stavy a v něm se podíváme na nejvýznamnější bit úplně vpravo. Jako listener skript tento snippet funguje nádherně.

Jedna z limitací je to, že si musíte vybrat předem jaké zařízení je na jakém vstupu. Zpětně měnit nelze. Pokud chci vstup Quida číslo 1 použít jako stavový kontakt, snippet šablony v XML vypadá takto. Navíc oproti třeba tlačítku je přiřazení do proměnné `Rc` a negace. Kontakt je totiž při stavu 1 uzavřen:

```xml
<Device>
    <Name>${bus_module_reed_contact}</Name>
    <Model>PacketParserReedContact</Model>
    <Id>-3</Id>
    <DeviceProperties>
    <DeviceType>0</DeviceType>
    <ReadScriptPacketParser></ReadScriptPacketParser>
    <InternalPollInterval>2500</InternalPollInterval>
    <CustomVariables>[]</CustomVariables>
    <ServiceAttributesScriptsPacketParser></ServiceAttributesScriptsPacketParser>
    <ServiceActionsScriptsPacketParser></ServiceActionsScriptsPacketParser>
    <ListenerScriptPacketParser>Rc := !GETBIT(GETAT(RECEIVEDBYTES,LENGTH(RECEIVEDBYTES) - 3), 0);</ListenerScriptPacketParser>
    <OnStateIconId>87</OnStateIconId>
    <OffStateIconId>88</OffStateIconId>
    <OnStateName>${general_open}</OnStateName>
    <OffStateName>${general_closed}</OffStateName>
    <ReadStateScriptPacketParser></ReadStateScriptPacketParser>
    </DeviceProperties>
</Device>
```

Celou šablonu teprve dávám dohromady.