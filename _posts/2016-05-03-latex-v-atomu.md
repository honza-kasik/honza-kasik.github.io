---
title: Latex v Atomu na Fedoře
---

Nedávno jsem kvůli psaní bakalářské práce a dalších věcí v LaTeXu hledal nějaký rozumný editor, který by mi v tom pomohl. Na editaci menších věcí v HTML nebo Markdown běžně používám Atom a po doporučení jsem se jej rozhodl použít i pro LaTeX.

Ve Fedoře 23 používám Atom z repozitáře [mosquito/atom](https://copr.fedorainfracloud.org/coprs/mosquito/atom/), ve kterém přibývají aktualizace rychleji než v [helber/atom](https://copr.fedorainfracloud.org/coprs/helber/atom/). Jako vhodný doplňek pro LaTeX se osvědčil https://github.com/thomasjo/atom-latex - nabízí širokou škálu nastavení – od vlastního LaTex builder přes prohlížeč PDF až po LaTeX engine.

Pro zprovoznění ve Fedoře jej stačí nainstalovat pomocí správce balíčků přítomného v Atomu nebo pomocí `apm install latex`. Pak už jen stačí v \*.tex dokumentu stisknout kombinaci Ctrl+Alt+B a dokument by se měl zkompilovat. Může se stát, že se zobrazí chyba. V tom případě zřejmě nemáte nainstalovanou distribuci TexLive ve své instalaci Fedory nebo vám jen chybí balíček `latexmk`, na kterém doplněk závisí a který není běžnou součástí distribuce.

Po doinstalování potřebných závislostí – LaTex distribuce a `latexmk` vše dobře funguje. Mě se navíc osvědčilo povolit možnost zkompilovaní dokumentu po uložení. Nutno také podotknout, že balíček obsahuje pouze nástroje pro usnadnění kompilace. Pokud tedy potřebujete zvýraznění syntaxe, nainstalujte si ještě atom balíček `language-latex` Ve výsledku je tedy Atom použitelným LaTeX editorem.
