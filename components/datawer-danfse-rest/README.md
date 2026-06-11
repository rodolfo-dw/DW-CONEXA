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
só traz o código. A cada geração de DANFSe o componente
(`IbgeMunicipios.java`) consulta a API oficial do IBGE
(`https://servicodados.ibge.gov.br/api/v1/localidades/municipios`) e monta o
de-para código → nome.

Se a API estiver fora, mantém o último de-para carregado com sucesso ou, na
primeira falha, usa o **snapshot embutido no JAR**
(`src/main/resources/danfse/municipios-ibge.txt`, linhas `codigo;nome`,
gerado a partir da mesma API). Não há setup: nenhum dataset, agendamento ou
credencial é necessário.

O de-para **nunca quebra a geração**: sem ele, cai nos nomes presentes no
próprio XML e, em último caso, imprime o código IBGE.

## Pendências (próximas fases)

- Autenticação do endpoint
- Conferência fina de fidelidade com o Anexo I da NT
