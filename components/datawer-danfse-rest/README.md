# Datawer - DANFSe API

Componente REST independente para geração do **DANFSe v2.0** (NT 008/2026 da SE/CGNFS-e)
a partir do XML da NFS-e Nacional. Substitui a API oficial de geração do DANFSe,
que será descontinuada em 1º de julho de 2026.

Para entrega a outro desenvolvedor, consulte o
[guia completo de build, deploy e integração](GUIA-INTEGRACAO.md).

## Build

```bash
mvn package
```

Gera `target/datawer.war`. Publicar no servidor de aplicação do Fluig (deploy de war).

## Endpoint

```
POST /datawer/api/v1/danfse/base64[?situacao=CANCELADA|SUBSTITUIDA][&debug=true]
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

Com `debug=true`, a resposta também inclui `audit`, com uma entrada por campo
impresso: bloco, label, valor, caminho XML, regra de formatação e indicadores de
traço por ausência, conversão por tabela e frase especial de bloco. Sem esse
parâmetro, o contrato da resposta permanece igual.

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
frequentemente traz apenas o código. O componente carrega uma vez o **snapshot
embutido no JAR** (`src/main/resources/danfse/municipios-ibge.txt`, linhas
`codigo;nome`) e faz o de-para de forma determinística, sem depender de rede.

A API oficial do IBGE
(`https://servicodados.ibge.gov.br/api/v1/localidades/municipios`) é usada apenas
como contingência caso o recurso local não esteja disponível. Não há setup,
agendamento ou credencial.

O de-para **nunca quebra a geração**: sem ele, usa os nomes presentes no próprio
XML; campo individual sem nome resolvido recebe `-`.

## Referência normativa

O gerador segue a [NT 008 - DANFSe, versão 2.0](https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/rtc/nt-008-se-cgnfse-danfse-20260505.pdf).
