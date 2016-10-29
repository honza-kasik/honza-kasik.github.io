---
title: Zmenšení velikosti .docx souboru v Linuxu
excerpt: Nedávno jsem byl požádán o radu, která se vztahovala k příliš velkému docx souboru. Soubor měl přes 22 MB a to už se může projevit na neochotě některých emailových serverů takový soubor přijmout.
---

![Folder with docx files](/assets/images/2015-12-05-docx_downsize_intro.png){: .img-intro }

Nedávno jsem byl požádán o radu, která se vztahovala k příliš velkému docx souboru. Soubor měl přes 22 MB a to už se může projevit na neochotě některých emailových serverů takový soubor přijmout. Samozřejmě, že existuje možnost takový soubor nahrát někam do cloudového úložiště, ale co když je dokument citlivý nebo o takové možnosti nevíme (a žijeme třeba na Marsu)?

V tomto případě se jednalo o seminární práci se spoustou obrázků, tudíž jsem usoudil, že na vině budou ony. V prvním kroku jsem soubor rozbalil pomocí `unzip -d dokument dokument.docx` do složky dokument.

Obrázky se v rozbaleném souboru nacházejí ve složce `/word/media/` a tak jsem se pomocí příkazu `cd dokument/word/media` do této složky přepnul. K mému překvapení byly všechny obrázky ve formátu png. Jelikož se jednalo převážně o screenshoty, byl tento formát podle mého názoru zvolen nevhodně.

Pomocí příkazu ze sbírky `imagemagick`, `mogrify -format jpg *.png` jsem vše převedl na formát jpg a odstranil původní png soubory. Velikost všech obrázků se dostala na třetinu a já byl spokojen.

Kdybych teď však zkusil soubor opět zkompletovat, narazil bych na chybu chybných referencí, protože jsem fakticky soubory přejmenoval a v dokumentu by se tudíž nezobrazily. Tento problém lze ale opět vyřešit velmi jednoduše. Stačí se navigovat do složky `/word/_rels` a v ní v souboru document.xml.rels změnit všechny výskyty řetězce `png` na `jpg` příkazem `sed -i.bak s/png/jpg/g document.xml.rels`, který zároveň vytvoří zálohu pro případ, že by se něco nepovedlo.

Posledním krokem byla tedy kompletace souboru zpět. K tomu stačilo přejít do složky dokument a spustit `zip -r ../smaller_dokument.docx`, vedle původního dokumentu se pak vytvořil nový, s téměr třetinovou velikostí. Ještě před kompletací můžete chtít odstranit vytvořené zálohy ;).
