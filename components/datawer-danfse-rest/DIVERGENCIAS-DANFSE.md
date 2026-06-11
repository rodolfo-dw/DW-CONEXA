# Divergências: DANFSe gerada × NT 008 (Anexo I)

> Análise de 11/06/2026 comparando `gerada.pdf` (saída do `DanfseGenerator`),
> `oficial.pdf` (baixada do portal gov) e a NT 008 (`ESPECIFICACAO-DANFSE.md`).
> Medições feitas por extração direta dos PDFs (fontes, espessuras, posições em cm).

## ✅ STATUS: corrigido em 11/06/2026 (revisado no mesmo dia)

Os itens 1–11 abaixo foram corrigidos no `DanfseGenerator.java`. TTFs em
`src/main/resources/danfse/fonts/` (copiados do macOS — conferir licenciamento
antes de distribuir fora da empresa). Permanecem em aberto apenas os "menores":
cores RGB (não CMYK) e nome do município via tabela IBGE (fase 2).

**Revisão (decisão do Rodolfo): o texto normativo prevalece sobre o desenho do
Anexo I.** Três itens que tinham sido implementados com base apenas no desenho
foram REVERTIDOS/REMOVIDOS por não constarem do texto da NT:
- grade interna tracejada entre células/linhas de campos (item 2.2.3 só prevê
  borda da página 1pt + divisórias de blocos 0,5pt);
- divisórias verticais do cabeçalho (logo/descrição/município);
- labels em caixa alta 7pt em "Valor da Operação / Serviço" e "Valor Líquido da
  NFS-e + IBS/CBS" (item 2.4.2 manda 6pt Primeira Letra Maiúscula fora do bloco
  de identificação; o sombreamento do segundo permanece, pois está no 2.2.3).

Estado validado por extração do PDF final: 1 rect 1pt (borda da página) +
12 rects 0,5pt (blocos) + 11 sombreamentos 5% + ZERO linhas internas;
Arial/MS Sans Serif embutidas como subset; corpo em 28,77.

## ⚠️ Contexto essencial: oficial.pdf é o modelo ANTIGO (v1.0)

A nota baixada do portal é **"DANFSe v1.0"**: sem sombreamento, labels Title Case,
conteúdo 8 pt, QR de 1,76 cm, sem blocos IBS/CBS. A NT 008 especifica o **v2.0**
(o que o nosso gerador produz). Grande parte do "não bate" entre gerada × oficial é
**diferença de versão de modelo, não erro nosso**. O gabarito do layout é o
**Anexo I da NT 008**. A oficial só serve de referência para a tipografia
(ela embute Arial e Microsoft Sans Serif reais).

## Divergências reais da gerada (em ordem de impacto visual)

### 1. Fontes erradas — Helvetica no lugar de Arial / Microsoft Sans Serif
- **Spec (2.4)**: Arial para títulos/labels, Microsoft Sans Serif para conteúdo.
- **Gerada**: 100% Helvetica/Helvetica-Bold (`FONT`/`BOLD` em `DanfseGenerator.java:55-56`).
- **Oficial**: embute `Arial` e `Microsoft Sans Serif` (subset TTF) de verdade.
- É a principal causa de "as letras não batem". Correção: embutir `arial.ttf`/
  `arialbd.ttf`/`micross.ttf` como resources via `PDType0Font.load()`.

### 2. Faixas de bloco suprimido: estilo errado
- **Spec (exemplos 2.4.5.1)**: texto **centralizado**, fundo **branco**, sem negrito aparente.
- **Gerada**: alinhado à esquerda, **sombreado de cinza**, negrito
  (`suppressedBlock`, `DanfseGenerator.java:530-537`). Além disso o `shade()` é
  pintado DEPOIS do `stroke()`, cobrindo metade da borda da faixa.

### 3. Bloco ISSQN: título como faixa de largura total (estrutura errada)
- **Anexo I**: 1ª linha do bloco = célula-título "TRIBUTAÇÃO MUNICIPAL (ISSQN)" (col. 0,30)
  + "Tipo de Tributação do ISSQN" (col. 5,41) + "Município/UF/País da Incidência" (col. 10,51).
- **Gerada**: faixa sombreada de 20,40 × 0,30 e os dois campos deslocados uma coluna
  à esquerda na linha seguinte (`blockStrip` em `drawIssqn`, `DanfseGenerator.java:358-365`).
  Isso também deixa o bloco 0,30 cm mais alto que o modelo.

### 4. Bordas duplas entre blocos (gaps de 0,02 cm)
- **Anexo I**: blocos contíguos — uma única linha divisória entre blocos.
- **Gerada**: cabeçalho termina em 1,46 e "Dados" começa em 1,48; "Dados" termina em
  4,32 e Prestador começa em 4,34 → linhas duplas com 0,02 cm de vão, visíveis.
  (Os demais blocos foram desenhados contíguos: 6,91/8,85/9,17… — ou seja, o próprio
  PDF mistura os dois estilos.) Padronizar tudo contíguo.

### 5. Valores monetários sem "R$ "
- **Anexo I**: placeholders **"R$"** nos campos monetários (e "%" nos percentuais).
- **Gerada**: imprime `1.221,22` sem o prefixo (`money()`, `DanfseGenerator.java:727`).
  Os percentuais já saem com "%" — ok. Adicionar prefixo `R$ `.

### 6. Complemento do QR Code em 4 linhas (spec: 3 linhas)
- **Spec (2.4.3)**: texto "A autenticidade…" disposto em **3 linhas**, quadro de
  0,68 × 4,72 em (15,80 / 3,36).
- **Gerada**: 4 linhas começando em y 3,42 com passo 0,22 → última baseline em 4,08,
  estourando o quadro (termina em 4,04) (`qrTxt`, `DanfseGenerator.java:186-195`).

### 7. Labels especiais do bloco "Valor Total da NFS-e"
- **Anexo I**: "VALOR DA OPERAÇÃO / SERVIÇO" e "VALOR LÍQUIDO DA NFS-e + IBS/CBS"
  aparecem em **caixa alta 7 pt** (estilo de título de bloco); o segundo é sombreado.
- **Gerada**: ambos como label comum 6 pt Title Case (sombreamento do segundo ok).

### 8. Faixa-título "INFORMAÇÕES COMPLEMENTARES" com altura menor
- **Spec**: Alt. **0,39 cm** (linha de conteúdo idem).
- **Gerada**: `blockStrip` fixa **0,30 cm** (`DanfseGenerator.java:553-557`).

### 9. "NFS-E" maiusculizado nos labels de identificação
- `fieldCaps` aplica `toUpperCase()` no label inteiro → "CHAVE DE ACESSO DA NFS-E"
  (`DanfseGenerator.java:588`). O Anexo I e os exemplos preservam a marca "NFS-e"
  com "e" minúsculo. Usar os labels já escritos em caixa alta com "NFS-e" literal.

### 10. Corpo se estende além do modelo
- **Modelo**: corpo termina em 28,77 cm (com ou sem canhoto, já que a supressão do
  canhoto é absorvida por Inf. Complementares no MESMO valor).
- **Gerada**: `BOTTOM = 29.40` (`DanfseGenerator.java:53`) — 0,63 cm além do previsto.
  As margens continuam válidas (≥ 0,15), mas o crescimento de Inf. Compl. excede o
  "mesmo valor da redução" das supressões. Recalcular: 28,77 + Σ(supressões da página).

### 11. Cabeçalho sem divisórias verticais
- **Anexo I**: linhas verticais separando logo / "DANFSe v2.0" / município-ambiente.
- **Gerada**: faixa única sem divisórias internas (`drawCabecalho`).

### Menores / a confirmar
- **Cores em RGB, não CMYK**: spec pede K100 (texto), K35 (marca d'água), 5% (sombra).
  Em tela é equivalente; para gráfica, considerar `setNonStrokingColor` CMYK.
- **Nome do município do tomador**: fase 1 imprime o código IBGE quando o XML não traz
  o nome (`municipioNome`, `DanfseGenerator.java:910`) — spec manda usar a descrição
  da tabela IBGE (já previsto para fase 2).
- **Município no cabeçalho**: tabela manda `Município: CCCC / CC` (como está); o
  Anexo I ilustra `CCCC - CC`. Mantido conforme tabela; ambiguidade documentada.
- **Chave de acesso**: altura 0,79 usada × 0,77 na tabela (passo até a linha seguinte
  é 0,79 na própria tabela — sem efeito visual).

## Conformidades verificadas (não mexer)

✓ A4 retrato, página única; borda da página 1 pt em 0,20 cm (margem dentro de 0,15–0,20)
✓ Linhas divisórias 0,5 pt
✓ Sombra 5% (RGB 242) no cabeçalho, títulos de blocos, Emitente e Valor Líquido+IBS/CBS
✓ Tamanhos de fonte: 9 pt cabeçalho, 8 pt município, 7 pt conteúdo/títulos/ident.,
  6 pt labels/QR — todos conferem com a spec
✓ QR Code 1,52 × 1,52 cm exatamente em (17,48 / 1,67), URL correta
✓ Colunas 0,30 / 5,41 / 10,51 / 15,62 e posições/alturas das linhas conforme tabela
✓ Logomarca 4,00 cm de largura em (0,49 / 0,44), proporção preservada
✓ Campos vazios com "-" (Nota 12); reticências nos limites de caracteres
✓ Formatos DD/MM/AAAA, CNPJ/CPF, NBS, cTribNac; pipes nas Inf. Complementares;
  linha fixa dos Totais Aproximados (Lei 12.741/2012)
✓ Marca d'água diagonal 60 pt (≥ 50) cinza 166 (K35) — só falta ser Arial (item 1)
