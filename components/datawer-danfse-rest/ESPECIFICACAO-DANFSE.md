# Especificação Técnica do DANFSe v2.0 — Referência de Layout

> Fonte: **Nota Técnica nº 008 – Versão 1.0 (05/05/2026)** — SE/CGNFS-e ("documentacao.pdf").
> Este arquivo consolida TODOS os parâmetros de layout para a geração do DANFSe.
> Onde a tabela do item 2.4.5 conflita com o modelo do Anexo I, o conflito está sinalizado —
> o item 2.2.4 diz que a **disposição dos campos deve obrigatoriamente obedecer ao Anexo I**.

---

## 1. Formulário (item 2.2)

| Parâmetro | Valor |
|---|---|
| Papel | A4 (210 × 297 mm), modo **retrato**, página única obrigatória |
| Margens (corpo ↔ borda do papel) | mín. **0,15 cm**, máx. **0,20 cm** em TODAS as laterais (inclusive sup./inf.) |
| Borda da página | **1 pt** de espessura |
| Linhas divisórias dos blocos | **0,5 pt** de espessura |
| Sombreamento (fundo cinza 5%) | Apenas em: **cabeçalho**, **títulos de cada bloco de campos**, campo **"Emitente da NFS-e"** e campo **"Valor Líquido da NFS-e + IBS/CBS"**. Demais campos: fundo branco (0%) |
| Cinza 5% em RGB | ≈ (242, 242, 242) — 0,95 |

## 2. Fontes (item 2.4)

Cores: texto **preto sólido (K100)**; espaçamento normal.

| Elemento | Fonte | Tamanho | Estilo |
|---|---|---|---|
| Títulos (labels) dos **blocos** | Arial | **7 pt** | **negrito, CAIXA ALTA** |
| Títulos (labels) dos **campos comuns** | Arial | **6 pt** | **negrito**, Primeira Letra Maiúscula |
| Labels do bloco de **Identificação da NFS-e** (item 2.1.2) | Arial | **7 pt** | **negrito, CAIXA ALTA** (manter "NFS-e" com "e" minúsculo, como no Anexo) |
| **Conteúdo** de todos os campos | Microsoft Sans Serif | **7 pt** | normal |
| Cabeçalho: "DANFSe v2.0" e "Documento Auxiliar da NFS-e" | Arial | **9 pt** | **negrito**, centralizado no quadro |
| Cabeçalho: "NFS-e SEM VALIDADE JURÍDICA" (só homologação, tpAmb=2) | Arial | **9 pt** | **negrito, vermelho M100/Y100** (RGB 255,0,0), abaixo de "Documento Auxiliar da NFS-e" |
| Cabeçalho: Município do emitente (canto direito) | Microsoft Sans Serif | **8 pt** | normal |
| Cabeçalho: Ambiente gerador + Tipo de ambiente | Microsoft Sans Serif | **6 pt** | normal |
| Complemento do QR Code | Microsoft Sans Serif | **6 pt** | normal, **em 3 (três) linhas** |
| Marca d'água CANCELADA / SUBSTITUÍDA | Arial | **≥ 50 pt** | normal, diagonal, **cinza K35** (RGB ≈ 166,166,166) |

⚠️ A NT exige **Arial** e **Microsoft Sans Serif** nominalmente. Helvetica é métrica e
visualmente parecida, mas NÃO é a fonte exigida — o PDF oficial do portal embute
Arial e Microsoft Sans Serif de verdade (subset TTF).

## 3. Cabeçalho (item 2.4.3)

O cabeçalho é dividido em **3 quadros com divisórias verticais** (ver Anexo I):

| Quadro | Alt. (cm) | Larg. (cm) | Esq. (cm) | Sup. (cm) | Conteúdo |
|---|---|---|---|---|---|
| CABEÇALHO (faixa toda, sombreada) | 1,16 | 20,40 | 0,30 | 0,30 | — |
| Logomarca NFS-e | 0,85 | 4,00 | 0,49 | 0,44 | Logo oficial (gov.br/nfse → "Logo - NFS-e - Horizontal.png") |
| Quadro da descrição | 1,16 | 10,19 | 5,41 | 0,30 | "DANFSe v2.0" + "Documento Auxiliar da NFS-e" centralizados |
| Quadro município/ambiente | 1,16 | 5,09 | 15,62 | 0,30 | ver abaixo |
| → Município + UF | 0,64 | 5,09 | 15,62 | 0,30 | Formato `Município: CCCC / CC` (xLocEmi + emit/enderNac/UF). Não exibir quando item do cód. trib. nacional = 99. (Obs.: Anexo I ilustra `... - CC`; a tabela manda `/ CC`) |
| → Ambiente gerador (ambGer) | 0,24 | 5,09 | 15,62 | 0,97 | descrição (1=Prefeitura, 2=Sistema Nacional NFS-e) |
| → Tipo de ambiente (tpAmb) | 0,24 | 5,09 | 15,62 | 1,22 | descrição (1=Produção, 2=Homologação) |

## 4. QR Code (item 2.4.3)

| Parâmetro | Valor |
|---|---|
| URL | `https://www.nfse.gov.br/ConsultaPublica/?tpc=1&chave=` + Chave de Acesso |
| Dimensões mínimas | **1,52 × 1,52 cm** |
| Posição (X/Y) | **X: 17,48 cm / Y: 1,67 cm** |
| Quadro do complemento | Alt. 0,68 × Larg. 4,72 cm em (15,80 / 3,36) |
| Texto do complemento | "A autenticidade desta NFS-e pode ser verificada pela leitura deste código QR ou pela consulta da chave de acesso no portal nacional da NFS-e" — **3 linhas**, 6 pt |

## 5. Tabela de posições dos campos (item 2.4.5)

Medidas em **cm**, origem no canto superior esquerdo da página. Colunas-padrão de
labels: **0,30 / 5,41 / 10,51 / 15,62**. Largura do corpo: **20,40** (0,30 → 20,70).
Conteúdos vazios no XML: imprimir **"-"** (Nota 12).

### 5.1 Dados da NFS-e (bloco em 0,30 / 1,48 — Alt. 2,84 × Larg. 20,40)

| Campo | XML | Alt. | Larg. | Esq. | Sup. | Formato / Obs. | Tam. |
|---|---|---|---|---|---|---|---|
| CHAVE DE ACESSO DA NFS-e | infNFSe/@id | 0,77 | 15,30 | 0,30 | 1,48 | id sem prefixo "NFS" (50 dígitos, bloco único) | 50 |
| NÚMERO DA NFS-e | infNFSe/nNFSe | 0,67 | 5,09 | 0,30 | 2,27 | | 13 |
| COMPETÊNCIA DA NFS-e | DPS/infDPS/dCompet | 0,67 | 5,09 | 5,41 | 2,27 | DD/MM/AAAA | 10 |
| DATA E HORA DA EMISSÃO DA NFS-e | infNFSe/dhProc | 0,67 | 5,09 | 10,51 | 2,27 | DD/MM/AAAA hh:mm:ss | 19 |
| NÚMERO DA DPS | DPS/infDPS/nDPS | 0,67 | 5,09 | 0,30 | 2,96 | | 15 |
| SÉRIE DA DPS | DPS/infDPS/serie | 0,67 | 5,09 | 5,41 | 2,96 | | 5 |
| DATA E HORA DA EMISSÃO DA DPS | DPS/infDPS/dhEmi | 0,67 | 5,09 | 10,51 | 2,96 | DD/MM/AAAA hh:mm:ss | 19 |
| EMITENTE DA NFS-e **(sombreado)** | DPS/infDPS/tpEmit | 0,67 | 5,09 | 0,30 | 3,65 | descrição (1=Prestador, 2=Tomador, 3=Intermediário) | 13 |
| SITUAÇÃO DA NFS-e | infNFSe/cStat | 0,67 | 5,09 | 5,41 | 3,65 | descrição; reticências se > 37 chars | 40 |
| FINALIDADE | DPS/infDPS/IBSCBS/finNFSe | 0,67 | 5,09 | 10,51 | 3,65 | descrição; reticências se > 37 chars | 40 |

### 5.2 Prestador / Fornecedor (título do bloco em 0,30 / 4,34)

| Campo | XML (prefixo DPS/infDPS/prest) | Alt. | Larg. | Esq. | Sup. | Obs. | Tam. |
|---|---|---|---|---|---|---|---|
| CNPJ / CPF / NIF | CNPJ \| CPF \| NIF | 0,63 | 5,09 | 5,41 | 4,34 | nn.nnn.nnn/nnnn-nn / nnn.nnn.nnn-nn | 18/14/40 |
| Indicador Municipal (Inscrição) | IM | 0,63 | 5,09 | 10,51 | 4,34 | | 15 |
| Telefone | fone | 0,63 | 5,09 | 15,62 | 4,34 | | 20 |
| Nome / Nome Empresarial | xNome | 0,63 | 10,19 | 0,30 | 4,98 | reticências se > 77 | 80 |
| Município / Sigla UF | end/endNac/cMun ou end/endExt/xCidade | 0,63 | 5,09 | 10,51 | 4,98 | nome IBGE + " / UF" | 37 |
| Código IBGE / CEP | cMun + CEP ou cEndPost | 0,63 | 5,09 | 15,62 | 4,98 | nnnnnnn / nn.nnn-nnn | 21 |
| Endereço (Nota 1) | xLgr, nro, xCpl, xBairro | 0,63 | 10,19 | 0,30 | 5,62 | concatenar com ", "; reticências > 77 | 80 |
| Email (Nota 1) | email | 0,63 | 10,19 | 10,51 | 5,62 | | 80 |
| Simples Nacional na Data de Competência | regTrib/opSimpNac | 0,63 | 5,09 | 0,30 | 6,28 | descrição; reticências > 37 | 40 |
| Regime de Apuração Tributária pelo SN | regTrib/regApTribSN | 0,63 | 10,19 | 10,51 | 6,28 | descrição; reticências > 77 | 80 |

### 5.3 Tomador / Adquirente (título em 0,30 / 6,92 — Nota 2)

Mesma grade do Prestador, sem a linha do Simples Nacional:
CNPJ (5,41/6,92) · IM (10,51/6,92) · Telefone (15,62/6,92) ·
Nome (0,30/7,56, larg. 10,19) · Município/UF (10,51/7,56) · IBGE/CEP (15,62/7,56) ·
Endereço (0,30/8,22, Nota 1) · E-mail (10,51/8,22, Nota 1). XML: `DPS/infDPS/toma`.

### 5.4 Destinatário da Operação (título em 0,30 / 8,86 — Notas 2 e 3)

CNPJ (5,41/8,86) · Telefone (15,62/8,86) — **sem IM** ·
Nome (0,30/9,50) · Município/UF (10,51/9,50) · IBGE/CEP (15,62/9,50) ·
Endereço (0,30/10,16, Nota 1) · E-mail (10,51/10,16, Nota 1). XML: `DPS/infDPS/IBSCBS/dest`.

### 5.5 Intermediário da Operação (título em 0,30 / 10,80 — Nota 2)

CNPJ (5,41/10,80) · IM (10,51/10,80) · Telefone (15,62/10,80) ·
Nome (0,30/11,44) · Município/UF (10,51/11,44) · IBGE/CEP (15,62/11,44) ·
Endereço (0,30/12,09, Nota 1) · E-mail (10,51/12,09, Nota 1). XML: `DPS/infDPS/interm`.

### 5.6 Serviço Prestado (título em 0,30 / 12,74)

| Campo | XML | Alt. | Larg. | Esq. | Sup. | Obs. | Tam. |
|---|---|---|---|---|---|---|---|
| Código de Tributação Nacional / Municipal | serv/cServ/cTribNac + infNFSe/cTribMun | 0,63 | 5,09 | 5,41 | 12,74 | nn.nn.nn / nnn | 14 |
| Código da NBS | serv/cServ/cNBS | 0,63 | 5,09 | 10,51 | 12,74 | n.nnnn.nn.nn | 9 |
| Local da Prestação / Sigla UF / País | infNFSe/xLocPrestacao + serv/locPrest/cPaisPrestacao | 0,63 | 5,09 | 15,62 | 12,74 | Município / UF / País (ISO 2 dígitos, ex.: BR) | 42 |
| Descrição do Cód. Trib. Nacional/Municipal | xTribMun se ≠ "" senão xTribNac | **0,38** | 20,40 | 0,30 | 13,39 | **SEM label**; reticências > 167 | 170 |
| Descrição do Serviço | serv/cServ/xDescServ | 0,63 | 20,40 | 0,30 | 13,79 | reticências > 1297 | 1300 |

### 5.7 Tributação Municipal — ISSQN (título em 0,30 / 14,43 — Nota 4)

⚠️ **Conflito tabela × Anexo I**: a tabela posiciona "Tipo de Tributação do ISSQN" em
Esq. 0,30 (sobreposto ao título) e "Município/UF/País da Incidência" em 5,41.
O **Anexo I mostra**: 1ª linha = [título do bloco] (0,30) + [Tipo de Tributação do ISSQN]
(5,41) + [Município / Sigla UF / País de Incidência do ISSQN] (10,51, até a borda).
**O Anexo I prevalece** (item 2.2.4). O título do ISSQN é uma CÉLULA na 1ª linha,
igual aos demais blocos — **não** uma faixa de largura total.

| Campo | XML | Esq. | Sup. | Obs. | Tam. |
|---|---|---|---|---|---|
| Tipo de Tributação do ISSQN | valores/trib/tribMun/tribISSQN | 5,41¹ | 14,43 | descrição (1=Operação Tributável…4=Não Incidência) | 21 |
| Município / Sigla UF / País da Incidência | infNFSe/xLocIncid + cPaisResult | 10,51¹ | 14,43 | Município / UF / País | 42 |
| Regime Especial de Tributação (Nota 5) | prest/regTrib/regEspTrib | 0,30 | 15,08 | descrições 0–6, 9 | 27 |
| Tipo de Imunidade do ISSQN (Nota 5) | tribMun/tpImunidade | 5,41 | 15,08 | reticências > 37 | 40 |
| Suspensão da Exigibilidade (Nota 5) | tribMun/exigSusp/tpSusp | 10,51 | 15,08 | "Exigibilidade Suspensa por Decisão Judicial/Processo Administrativo" | 40 |
| Número Processo Suspensão (Nota 5) | tribMun/exigSusp/nProcesso | 15,62 | 15,08 | | 30 |
| Benefício Municipal (Nota 5) | infNFSe/valores/tpBM | 0,30 | 15,73 | descrição | 40 |
| Cálculo do BM (Nota 5) | vCalcBM ou vRedBCBM | 5,41 | 15,73 | monetário | 1-15V2 |
| Total Deduções/Reduções (Nota 5) | vDR ou vCalcDR + vCalcReeRepRes | 10,51 | 15,73 | monetário | 1-15V2 |
| Desconto Incondicionado (Nota 5) | vDescIncond | 15,62 | 15,73 | monetário | 1-15V2 |
| BC ISSQN | infNFSe/valores/vBC | 0,30 | 16,37 | monetário (R$) | 1-15V2 |
| Alíquota Aplicada | infNFSe/valores/pAliqAplic | 5,41 | 16,37 | percentual (%) | 1-2V2 |
| Retenção do ISSQN | tribMun/tpRetISSQN | 10,51 | 16,37 | descrição (1=Não Retido…) | 25 |
| ISSQN Apurado | infNFSe/valores/vISSQN | 15,62 | 16,37 | monetário (R$) | 1-15V2 |

¹ posições conforme Anexo I (ver conflito acima). Todas as linhas: Alt. 0,63.

### 5.8 Tributação Federal — exceto CBS (título em 0,30 / 17,02)

| Campo | XML (prefixo valores/trib/tribFed) | Esq. | Sup. | Tam. |
|---|---|---|---|---|
| IRRF | vRetIRRF | 5,41 | 17,02 | 1-15V2 |
| Contribuição Previdenciária – Retida | vRetCP | 10,51 | 17,02 | 1-15V2 |
| Contribuições Sociais – Retidas | vRetCSLL | 15,62 | 17,02 | 1-15V2 |
| PIS – Débito Apuração Própria (Nota 6) | piscofins/vPis | 0,30 | 17,67 | 1-15V2 |
| COFINS – Débito Apuração Própria (Nota 6) | piscofins/vCofins | 5,41 | 17,67 | 1-15V2 |
| Descrição Contrib. Sociais – Retidas (Nota 6) | piscofins/tpRetPisCofins | 10,51 (larg. 10,19) | 17,67 | 35 |

### 5.9 Tributação IBS / CBS (título em 0,30 / 18,32)

| Campo | XML | Esq. | Sup. | Formato | Tam. |
|---|---|---|---|---|---|
| CST / cClassTrib | DPS…/IBSCBS/valores/trib/gIBSCBS | 5,41 | 18,32 | nnn / nnnnnn | 12 |
| Indicador de Operação / Cód. IBGE Incidência / Município Incidência / Sigla UF | cIndOp + infNFSe/IBSCBS/cLocalidadeIncid + xLocalidadeIncid | 10,51 (larg. 10,19) | 18,32 | nnnnnn / nnnnnnn / ccc / CC | 56 |
| Exclusões e Reduções da Base de Cálculo | Σ vDescIncond + vCalcReeRepRes + vISSQN + vPIS + vCOFINS | 0,30 | 18,96 | monetário | 1-15V2 |
| Base de Cálculo Após Exclusões e Reduções | IBSCBS/valores/vBC | 5,41 | 18,96 | monetário | 1-15V2 |
| Red. Alíquota IBS / Red. Alíquota CBS | pRedAliqUF + pRedAliqMun + pRedAliqCBS | 10,51 | 18,96 | % / % / % | 1-2V2 ×3 |
| Alíquota – IBS UF / IBS Mun | pIBSUF + pIBSMun | 15,62 | 18,96 | % / % | 1-2V2 ×2 |
| Alíq. Efetiva Municipal – IBS | valores/mun/pAliqEfetMun | 0,30 | 19,61 | % | 1-2V2 |
| Valor Apurado Municipal – IBS | totCIBS/gIBS/gIBSMunTot/vIBSMun | 5,41 | 19,61 | R$ | 1-15V2 |
| Alíq. Efetiva Estadual – IBS | valores/uf/pAliqEfetUF | 10,51 | 19,61 | % | 1-2V2 |
| Valor Apurado Estadual – IBS | totCIBS/gIBS/gIBSUFTot/vIBSUF | 15,62 | 19,61 | R$ | 1-15V2 |
| Valor Total Apurado – IBS | totCIBS/gIBS/vIBSTot | 0,30 | 20,26 | R$ | 1-15V2 |
| Alíquota – CBS | valores/fed/pCBS | 5,41 | 20,26 | % | 1-2V2 |
| Alíquota Efetiva – CBS | valores/fed/pAliqEfetCBS | 10,51 | 20,26 | % | 1-2V2 |
| Valor Total Apurado – CBS | totCIBS/gCBS/vCBS | 15,62 | 20,26 | R$ | 1-15V2 |

### 5.10 Valor Total da NFS-e (título em 0,30 / 20,90 — Alt. das linhas 0,67)

| Campo | XML | Esq. | Sup. | Obs. |
|---|---|---|---|---|
| Valor da Operação / Serviço | DPS…/valores/vServPrest/vServ | 5,41 | 20,90 | label 6pt comum (ver §9 — caps do desenho não se aplica) |
| Desconto Incondicionado | vDescIncond | 10,51 | 20,90 | |
| Desconto Condicionado | vDescCond | 15,62 | 20,90 | |
| Total das Retenções (ISSQN / Federais) | infNFSe/valores/vTotalRet | 0,30 | 21,59 | |
| Valor Líquido da NFS-e | infNFSe/valores/vLiq | 5,41 | 21,59 | |
| Total do IBS/CBS | vIBSTot + vCBS | 10,51 | 21,59 | |
| Valor Líquido da NFS-e + IBS/CBS | IBSCBS/totCIBS/vTotNF | 15,62 | 21,59 | **SOMBREADO** (item 2.2.3); label 6pt comum |

### 5.11 Informações Complementares (faixa-título em 0,30 / 22,27 — Alt. **0,39** × 20,40)

Conteúdo em 0,30 / 22,68 (Alt. 0,39 por linha, larg. 20,40, máx. 2000 chars; reticências
se > 1997, sem prejuízo à linha fixa dos Totais).

Ordem dos itens, separados por **pipes ( | )**:
`Inf. Cont.:` → `NFS-e Subst.:` (Nota 7) → `Doc. Ref.:` → `Cod. Obra:` (Nota 8) →
`Insc. Imob.:` (Nota 8) → `Cod. Evt.:` (Nota 9) → `Doc. Tec.:` → `Núm. Ped.:` →
`Item Ped.:` → `Inf. A. T. Mun.:`

Linha fixa obrigatória (Nota 10):
`Totais Aproximados dos Tributos cfe. Lei nº 12.741/2012: Federais: R$ ou % ; Estaduais: R$ ou % ; Municipais: R$ ou %`

### 5.12 Canhoto — opcional (faixa em 0,30 / 28,10 — Alt. 0,67 × 20,40 — Nota 11)

| Campo | Esq. | Larg. | Obs. |
|---|---|---|---|
| Data Cientificação | 0,30 | 5,09 | |
| Identificação e Assinatura | 5,41 | 5,09 | |
| Nº NFS-e / Chave NFS-e | 10,51 | 10,19 | id sem prefixo "NFS"; ex.: nnn / nnn (Tam. 66) |

**Fim do corpo com canhoto: 28,77 cm.** Se suprimido (item 2.3.3), "Descrição do
Serviço" e/ou "Informações Complementares" crescem no mesmo valor da supressão.

## 6. Supressões e modificações permitidas (item 2.3 + Notas 2/3/4)

Faixas de bloco suprimido — conforme **exemplos do item 2.4.5.1**:
- Altura mínima **0,32 cm**, largura **20,40 cm**;
- Texto **CENTRALIZADO** na largura, **sem sombreamento** (fundo branco);
- Mensagens exatas:
  - `TOMADOR/ADQUIRENTE DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e`
  - `DESTINATÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e`
  - `O DESTINATÁRIO É O PRÓPRIO TOMADOR/ADQUIRENTE DA OPERAÇÃO` (quando dest = toma)
  - `INTERMEDIÁRIO DA OPERAÇÃO NÃO IDENTIFICADO NA NFS-e`
  - `TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN`
- O espaço liberado vai para "Descrição do Serviço" e/ou "Informações Complementares",
  no mesmo valor da redução; coordenadas Y dos blocos seguintes são reajustadas.

## 7. Notas da tabela (item 2.4.5)

1. Linha Endereço/Email pode ser suprimida mesmo havendo dados no XML.
2. Tomador/Destinatário/Intermediário não informados → faixa de supressão (ver §6).
3. Destinatário = Tomador → faixa "O DESTINATÁRIO É O PRÓPRIO…".
4. Operação sem ISSQN → faixa "TRIBUTAÇÃO MUNICIPAL (ISSQN) - OPERAÇÃO NÃO SUJEITA AO ISSQN".
5. Linha suprimível se TODOS os campos da linha estiverem vazios no XML.
6. Linha PIS/COFINS impressa para competência até o fim do ano-calendário 2026.
7. Substituição → `NFS-e Subst.:` + chave da NFS-e substituída (chSubstda).
8. Obra/Imóvel → `Cod. Obra:` / `Insc. Imob.:` (cObra / inscImobFisc).
9. Evento → `Cod. Evt.:` (idAtvEvt).
10. Totais Aproximados dos Tributos: obrigatório, monetário OU percentual.
11. Canhoto é opcional.
12. **Campos sem informação no XML → preencher com traço (-).**

## 8. Marca d'água (itens 2.5.1 / 2.5.2)

- Cancelamento: texto **CANCELADA**; Substituição: texto **SUBSTITUÍDA**.
- Diagonal, formato normal, **mínimo 50 pt**, fonte **Arial**, **cinza K35**.
- O cancelamento NÃO consta no XML da NFS-e — chega por evento/parâmetro externo.

## 9. Observações de interpretação (Anexo I × tabela × texto)

**Critério adotado (decisão de 11/06/2026): o TEXTO NORMATIVO prevalece.** O desenho
do Anexo I vale para a DISPOSIÇÃO dos campos (item 2.2.4), mas detalhes gráficos que
só aparecem no desenho e não constam do texto são tratados como artefatos da
ferramenta usada para desenhar o modelo e **não devem ser impressos**:

- ❌ **Grade interna tracejada** (linhas verticais entre células e horizontais entre
  linhas de campos): aparece no desenho, mas o item 2.2.3 lista apenas borda da página
  (1 pt) e linhas divisórias dos blocos (0,5 pt). A DANFSe v1.0 do portal também não
  imprime grade interna. → NÃO desenhar.
- ❌ **Divisórias verticais do cabeçalho** (logo / descrição / município): mesmos
  motivos. → NÃO desenhar.
- ❌ **Labels em caixa alta no bloco "Valor Total"** ("VALOR DA OPERAÇÃO / SERVIÇO" e
  "VALOR LÍQUIDO DA NFS-e + IBS/CBS" aparecem em caps no desenho): o item 2.4.2 manda
  6 pt negrito Primeira Letra Maiúscula para labels fora do bloco de identificação.
  → seguir o item 2.4.2 (o sombreamento do "Valor Líquido da NFS-e + IBS/CBS" fica,
  pois ESTE está no texto do item 2.2.3).

Demais observações:

1. **Anexo I prevalece na disposição dos campos** (item 2.2.4) — ex.: bloco ISSQN tem o
   título como célula na 1ª linha (a tabela 2.4.5 contradiz a si mesma nesse ponto).
2. No Anexo I os **blocos são contíguos** (uma única linha entre blocos). A tabela tem
   "gaps" de 0,02 cm (1,46→1,48; 4,32→4,34) — não desenhar linhas duplas.
3. Campos monetários aparecem no Anexo I com placeholder **"R$"** e percentuais com
   **"%"** → conteúdo `R$ n.nnn,nn` e `n,nn%`. Mantido por ser CONTEÚDO indicado no
   modelo (não decoração) e por a Nota 10 e o v1.0 oficial usarem "R$".
4. Faixas de bloco suprimido: texto centralizado, fundo branco, peso normal — conforme
   os exemplos do item 2.4.5.1 (parte do texto da NT).
5. "DANFSe v1.0" (gerado hoje pelo portal nacional) é o modelo ANTIGO: sem sombreamento,
   conteúdo 8 pt, QR 1,76 cm, sem blocos IBS/CBS. Não usar como gabarito do v2.0 — vale
   apenas como referência de FONTES reais (Arial + Microsoft Sans Serif embutidas).
6. A API oficial de geração do DANFSe será suspensa em **1º/07/2026** — a partir daí a
   emissão segue exclusivamente esta NT (modelo v2.0).
