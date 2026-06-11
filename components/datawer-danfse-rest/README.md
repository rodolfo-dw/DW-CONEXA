# Datawer - DANFSe API

Componente REST independente para geração do **DANFSe v2.0** (NT 008/2026 da SE/CGNFS-e)
a partir do XML da NFS-e Nacional. Substitui a API oficial de geração do DANFSe,
que será descontinuada em 1º de julho de 2026.

## Build

```bash
mvn package
```

Gera `target/datawer.war`. Publicar no servidor de aplicação do Fluig (deploy de war).

## Endpoint

```
POST /datawer/api/v1/danfse/base64[?situacao=CANCELADA|SUBSTITUIDA]
Content-Type: application/xml
Body: XML completo da NFS-e (raiz <NFSe>)
```

Resposta:

```json
{
  "success": true,
  "chaveAcesso": "3304557222...",
  "numeroNfse": "53444",
  "emitente": "CONEXA SAUDE SERVICOS MEDICOS S.A.",
  "situacao": "NORMAL",
  "fileName": "DANFSe-53444.pdf",
  "contentType": "application/pdf",
  "pdfBase64": "JVBERi0xLjQK..."
}
```

Quem chama decide o que fazer com o base64: exibir no navegador ou publicar no GED.

### Exemplo

```bash
curl -X POST "https://SEU_FLUIG/datawer/api/v1/danfse/base64" \
  -H "Content-Type: application/xml" \
  --data-binary @nota.xml
```

## Marca d'água de cancelamento

O XML da NFS-e Nacional **não** indica cancelamento (o `cStat` permanece 100; o
cancelamento é um *evento* separado no Sistema Nacional). Por isso a situação é
informada pelo chamador via query param `situacao=CANCELADA` ou
`situacao=SUBSTITUIDA`, que imprime a marca d'água diagonal exigida pelo item 2.5
da NT.

## De-para código IBGE → nome do município

A tabela 2.4.5 manda imprimir o **nome** do município (tabela IBGE), mas o XML
só traz o código. O de-para vem do dataset sincronizado
**`ds_dw_api_ibge_municipios`** (fonte em `datasets/` na raiz do repositório),
que espelha a API oficial do IBGE
(`https://servicodados.ibge.gov.br/api/v1/localidades/municipios`).

O JSON completo (~2,4 MB) é quebrado em **linhas de 2.000 caracteres** na
coluna `JSON`, ordenadas pela coluna `CODIGO` (`0001`, `0002`, ...) com a
quantidade de partes na linha `TOTAL`. O tamanho respeita o menor limite de
coluna TEXT entre os bancos suportados pelo Fluig — Oracle: 4.000 **bytes**
(acentos em UTF-8 ocupam 2 bytes, então 2.000 chars cabem com folga); MySQL:
64 KB; SQL Server: sem limite prático. O consumidor lê todas as linhas,
concatena na ordem e remonta o JSON.

Setup (uma vez):

1. Publicar o dataset `ds_dw_api_ibge_municipios` no Fluig (Studio ou Painel de
   Controle → Datasets);
2. Painel de Controle → Datasets → coluna *Sincronização* → ativar
   **Sincronizar com o servidor** e agendar a tarefa (sugestão: 1x por dia).
   O dataset é jornalizado: o `onSync` atualiza sempre a mesma linha
   (`addOrUpdateRow`) e, se a API do IBGE estiver fora, mantém a última
   sincronização;
3. Configurar no JBoss do componente (system property ou variável de ambiente):
   - `datawer.fluig.url` / `DATAWER_FLUIG_URL` — default `http://localhost:8080`
   - `datawer.fluig.user` / `DATAWER_FLUIG_USER` — usuário de integração
   - `datawer.fluig.pass` / `DATAWER_FLUIG_PASS`

O componente (`IbgeMunicipios.java`) consulta o dataset pela API pública
`/api/public/ecm/dataset/datasets` (Basic auth), guarda o mapa em cache por 24h
e **nunca quebra a geração**: sem o dataset, cai nos nomes presentes no próprio
XML e, em último caso, imprime o código IBGE.

## Pendências (próximas fases)

- Autenticação do endpoint
- Conferência fina de fidelidade com o Anexo I da NT
