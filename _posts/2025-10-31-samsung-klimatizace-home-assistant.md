---
title: Integrace Samsung klimatizace do Home Assistant — po drátu
excerpt: Nikde jsem nenašel ucelený návod jak dostat Samsung klimatizaci po drátu do Home Assistant. Přehled toho, co jsem zjistil na jednom místě.
tags:
- smarthome
- home-assistant
- samsung
---

Chtěl jsem spolehlivé ovládání multisplitu bez cloudu a internetu. Rozhodl jsem se pro lokální Modbus řešení.

## Co budeme potřebovat

* Samsung MIM-B19N jednotku – Poskytuje Modbus RTU rozhraní a připojuje se k proprietární sběrnici venkovní jednotky.

* Modbus RTU/TCP převodník – Osobně používám tento čtyřkanálový od Waveshare, protože k němu mám připojené elektroměry a relé [Waveshare 4 channel RS485 to Ethernet server](https://www.aliexpress.com/item/1005006402111790.html). V síti se tváří jako 4 samostatné rozhraní. U každého lze nastavit jiné časování/paritu/stop bits pro Modbus RTU.

* Běžící Home Assistant s vhodnou konfigurací – Plně aktualizovaný.

## Schéma zapojení

![Diagram zpaojení](/assets/images/2025-10-31-samsung-modbus_diagram.png)

* Modbus RTU mám vedenou přes jeden pár cat7 kabelu.
* Ethernet jsou běžné kabely.
* Switch je PoE, napájím z něj i převodník.

## Home Assistant konfigurace

Veškerou konfiguraci mám v samostatném souboru `modbus.yaml` ve adresáři s konfigurací Home Assistenta. V `configuration.yaml` ji takto vkládám:

```yaml
modbus: !include modbus.yaml
```

V souboru `modbus.yaml` nejdříve musíme nakonfigurovat "hub" - blok konfguace pod kterým jsou přístupné jednotlivé entity. Je potřeba nakonfigurovat IP adresu převodníku a port.

`delay`, `timeout` a `message_wait_milliseconds` jsou hodnoty získané metodou pokus omyl, které se mi osvědčily:

```yaml
- name: samsung-ac
  type: tcp
  host: 192.168.1.42
  port: 4196
  delay: 0
  timeout: 2
  message_wait_milliseconds: 30
  ```

  Každá vnitřní jednotka pak definuje svůj vlastní `climates` prvek:

```yaml
- name: samsung-ac
  type: tcp
  host: 192.168.1.42
  port: 4196
  delay: 0
  timeout: 2
  message_wait_milliseconds: 30
  climates:
    - name: "Vnitřní jednotka tajné doupě"
      unique_id: vnitrni_jednotka_tajne_doupe
      slave: 1
      hvac_onoff_register: 52
      address: 59
      target_temp_register: 58
      scale: 0.1
      hvac_mode_register: 
        address: 53
        values:
        state_auto: 0
        state_cool: 1
        state_dry: 2
        state_fan_only: 3
        state_heat: 4
      fan_mode_register:
        address: 54
        values:
        state_fan_auto: 0
        state_fan_low: 1
        state_fan_medium: 2
        state_fan_high: 3
```

Vnitřní jednotka obsadí vždy 50 registrů a první začíná na registru 50 (číslováno od 0). Každá další začíná na i*50. Je ale možné, že jednotka prostě přeskočí očekávaný rozsah a je přístupná až na konci – místo 100 až 150 obsadí 200 až 250.

Čísla registrů a jejich hodnoty vychází z dokumentace k MIM-B19N a pravěpodobně se nezmění.

## Nastavení vnitřních jednotek

Pokud jste vše rozběhali může vám přijít divné, že data jdou sice číst, ale nemůžete jednotky ovládat. Je to proto, že Samsung jednotky mají ve výchozím stavu zakázané centrální ovládání. Nejjednoduší způsob jak ho povolit je pomocí originálního IR ovladače.

Doporučuji [toto super video jak zadat ovládací příkazy](https://www.youtube.com/watch?v=Uf7oTKOVEME). Ve zkratce:

1. Vyjměte jednu baterii z ovladače.
2. Stiskněte na ovladači tlačítko vypnutí, ovladač se vypne.
3. Stiskněte zároveň tlačítka na snížení a zvýšení teploty – jde to i jedním prstem – a vložte baterii.
4. Ovladač se spustí v režimu zadávání příkazů.
5. Přepínačem intenzity ventilátoru zadejte na první obrazovce _d2_. To je označení "direct" příkazů ke změně pouze jednoho parametru. Jinak se zadává celá konfigurace a to je děsná otrava.
6. Stiskněte tlačítko "mode". Dostanete se na další obrazovku.
7. Zadejte _05_. To je číslo segmentu, kde se nachází konfigurace, kterou chceme změnit. V tomto případě povolení centrálního ovládání.
8. Stiskněte tlačítko "mode".
9. Na třetí obrazovce zadejte _10_. _1_ je hodnota, kterou chceme zapsat. _0_ je vata.
10. Namiřte ovladač na jednotku a stiskněte tlačítko zapnout. Ozve se melodie zapnutí.
11. Stiskněte tlačítko zapnout znovu až se ozve krátký zvuk. To je potvrzení, že se už nic nezměnilo a hodnota je zapsaná.
12. Takto můžete oběhnout všechny jednotky.
13. Ovladač se vrátí zpět do běžného režimu cyklem: vyndání jedné baterky, stisk tlačítka vypnout, vložením jedné baterky.

Ještě jednou: kód je d2-05-10.

Tak a to je vše. Vše by mělo krásně fungovat.