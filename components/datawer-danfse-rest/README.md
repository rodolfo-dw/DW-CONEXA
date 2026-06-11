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

## Pendências (próximas fases)

- Tabela IBGE de municípios (hoje, quando o nome não consta no XML, imprime o código)
- Autenticação do endpoint
- Conferência fina de fidelidade com o Anexo I da NT
