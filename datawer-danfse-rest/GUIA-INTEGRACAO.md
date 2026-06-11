# Guia de build, deploy e integração - Datawer DANFSe API

## 1. Objetivo

Este componente recebe o XML completo de uma NFS-e Nacional e devolve o
Documento Auxiliar da NFS-e (DANFSe) v2.0 em PDF, codificado em Base64 dentro
de uma resposta JSON.

A implementação segue a **NT 008 - DANFSe v2.0**, publicada pela SE/CGNFS-e:

https://www.gov.br/nfse/pt-br/biblioteca/documentacao-tecnica/rtc/nt-008-se-cgnfse-danfse-20260505.pdf

O componente não usa a DANFSe municipal v1.0 como regra de campos ou de
layout.

## 2. Visão técnica

| Item | Definição |
| --- | --- |
| Linguagem | Java 11 |
| Empacotamento | WAR |
| API | JAX-RS / Java EE 8 (`javax.ws.rs`) |
| Context root | `/datawer` |
| Base path JAX-RS | `/api/v1` |
| Endpoint | `/danfse/base64` |
| Geração do PDF | Apache PDFBox |
| QR Code | ZXing |
| Banco de dados | Não utiliza |
| Variáveis de ambiente | Não exige |
| Arquivo gerado | `target/datawer.war` |

As fontes, a logomarca e o cadastro de municípios IBGE necessários para a
geração estão empacotados no WAR.

## 3. Pré-requisitos para build

- JDK 11 configurado em `JAVA_HOME`.
- Apache Maven 3.x.
- Acesso ao repositório Maven na primeira compilação para baixar dependências.
- Servidor compatível com Java EE 8/JAX-RS para executar o WAR. O projeto está
  preparado para JBoss/WildFly, inclusive com `jboss-web.xml`.

Conferência rápida:

```bash
java -version
mvn -version
```

O compilador exibido pelo Maven deve apontar para Java 11.

## 4. Compilação e testes

A partir da pasta `components/datawer-danfse-rest`:

```bash
mvn clean test
mvn clean package
```

O segundo comando também executa os testes antes de montar o WAR. Ao final, o
artefato estará em:

```text
target/datawer.war
```

A suíte automatizada valida, entre outros pontos:

- geração de PDF A4 com uma página para o XML de referência;
- valores monetários esperados;
- municípios e UFs obtidos por código IBGE;
- código NBS formatado;
- campos ausentes com `-`;
- frases especiais de blocos suprimidos;
- preservação do horário informado no XML;
- prioridade das tags totalizadoras;
- QR Code com a URL oficial e chave sem o prefixo `NFS`.

## 5. Deploy do WAR

Publique `target/datawer.war` pelo processo normal do servidor de aplicação,
por exemplo pelo console administrativo do JBoss/WildFly ou pelo mecanismo de
deploy adotado no ambiente Fluig.

O contexto é fixado em `/datawer`. Depois do deploy, o endpoint completo será:

```text
https://SEU_HOST/datawer/api/v1/danfse/base64
```

Em ambientes com proxy reverso ou prefixo adicional, ajuste somente a parte
anterior a `/datawer`.

Teste a disponibilidade fazendo uma chamada POST com um XML válido. O
componente não possui endpoint de health check dedicado.

## 6. Contrato da API

### Requisição

```http
POST /datawer/api/v1/danfse/base64
Content-Type: application/xml
Accept: application/json
```

O corpo deve conter o **XML completo e original da NFS-e**, começando pela raiz
`<NFSe>`. A implementação e os testes atuais utilizam o leiaute nacional XML
`versao="1.01"`. Não enviar o XML dentro de JSON e não codificá-lo em Base64.

O XML pode conter as assinaturas digitais recebidas do Sistema Nacional; elas
não precisam ser removidas para a geração do DANFSe.

Também são aceitos os content types `text/xml` e `text/plain`, mas
`application/xml` é o recomendado.

### Parâmetros de query string

| Parâmetro | Obrigatório | Valores | Comportamento |
| --- | --- | --- | --- |
| `situacao` | Não | `CANCELADA` ou `SUBSTITUIDA` | Imprime a marca d'água correspondente |
| `debug` | Não | `true` ou `false` | Inclui a auditoria dos campos no JSON |

Sem `situacao`, a resposta informa `NORMAL` e não aplica marca d'água.

O cancelamento não é deduzido de `cStat`, pois no padrão nacional ele é um
evento separado. Portanto, o sistema chamador deve consultar/controlar o evento
e enviar `situacao=CANCELADA` quando necessário.

Use somente os valores documentados. Atualmente um valor diferente não gera
erro, mas também não produz uma marca d'água válida.

### Resposta de sucesso

HTTP `200 OK`:

```json
{
  "success": true,
  "chaveAcesso": "33045572227092748000118000000005344426065793314288",
  "numeroNfse": "53444",
  "emitente": "CONEXA SAUDE SERVICOS MEDICOS S.A.",
  "situacao": "NORMAL",
  "fileName": "DANFSe-53444.pdf",
  "contentType": "application/pdf",
  "pdfBase64": "JVBERi0xLjQK..."
}
```

Para obter os bytes do PDF, decodifique `pdfBase64` usando Base64 padrão.

### Resposta com auditoria

Chamada:

```text
POST /datawer/api/v1/danfse/base64?debug=true
```

Além dos campos normais, a resposta terá o array `audit`:

```json
{
  "audit": [
    {
      "bloco": "SERVIÇO PRESTADO",
      "labelImpresso": "Código da NBS",
      "valorImpresso": "1.2301.22.00",
      "caminhoXmlOrigem": "NFSe/infNFSe/DPS/infDPS/serv/cServ/cNBS",
      "regraFormatacaoAplicada": "Formatação n.nnnn.nn.nn",
      "usouTracoPorAusencia": false,
      "usouDescricaoConvertidaPorTabela": false,
      "usouFraseEspecialDeBloco": false
    }
  ]
}
```

O modo debug é útil para homologação e diagnóstico. Ele não altera o PDF, mas
aumenta o tamanho da resposta; por isso não precisa ser utilizado em todas as
chamadas de produção.

### Resposta de erro

Formato:

```json
{
  "success": false,
  "message": "Descrição do erro"
}
```

Status relevantes:

| HTTP | Situação |
| --- | --- |
| `400` | Corpo vazio ou documento cuja raiz não é `NFSe` |
| `500` | XML malformado, falha de geração ou erro interno |

O consumidor deve verificar o status HTTP antes de acessar `pdfBase64` e tratar
qualquer resposta não 2xx como falha.

## 7. Exemplos de chamada

### cURL - nota normal

```bash
curl --fail-with-body \
  -X POST "https://SEU_HOST/datawer/api/v1/danfse/base64" \
  -H "Content-Type: application/xml" \
  -H "Accept: application/json" \
  --data-binary @nota.xml
```

### cURL - nota cancelada com auditoria

```bash
curl --fail-with-body \
  -X POST "https://SEU_HOST/datawer/api/v1/danfse/base64?situacao=CANCELADA&debug=true" \
  -H "Content-Type: application/xml" \
  -H "Accept: application/json" \
  --data-binary @nota.xml
```

### JavaScript

```javascript
async function gerarDanfse(xml, situacao) {
  const query = situacao ? `?situacao=${encodeURIComponent(situacao)}` : "";
  const response = await fetch(`/datawer/api/v1/danfse/base64${query}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/xml",
      "Accept": "application/json"
    },
    body: xml
  });

  const result = await response.json();
  if (!response.ok || !result.success) {
    throw new Error(result.message || `Erro HTTP ${response.status}`);
  }

  const binary = atob(result.pdfBase64);
  const bytes = Uint8Array.from(binary, char => char.charCodeAt(0));
  return new Blob([bytes], { type: result.contentType });
}
```

### Java 11

```java
String endpoint = "https://SEU_HOST/datawer/api/v1/danfse/base64";
String xml = Files.readString(Path.of("nota.xml"), StandardCharsets.UTF_8);

HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
        .header("Content-Type", "application/xml")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(xml, StandardCharsets.UTF_8))
        .build();

HttpResponse<String> response = HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

if (response.statusCode() / 100 != 2) {
    throw new IllegalStateException("Falha DANFSe: " + response.body());
}
```

Depois, o cliente deve ler `pdfBase64` com a biblioteca JSON já usada pelo
projeto consumidor e executar `Base64.getDecoder().decode(...)`.

## 8. Regras importantes de funcionamento

- Os dados impressos vêm do XML ou de conversões permitidas pela NT 008.
- Campos individuais sem informação são representados por `-`.
- Blocos suprimíveis usam as frases especiais previstas na documentação.
- Tags totalizadoras do XML têm prioridade; não são recalculadas.
- Somente totais cuja soma é expressamente prevista são calculados.
- Datas e horários preservam o horário textual da tag, sem conversão de fuso.
- Textos livres, como `xDescServ` e `xInfComp`, não são reescritos.
- O conteúdo pode sofrer quebra de linha ou truncamento exclusivamente visual.
- O QR Code aponta para a consulta pública nacional usando a chave sem `NFS`.
- O cadastro municipal é resolvido primeiro pelo snapshot IBGE incluído no WAR.

## 9. Validação e segurança

O componente protege o parser contra XXE, mas possui limites de
responsabilidade importantes:

- não valida a assinatura digital da NFS-e;
- não consulta a situação fiscal ou eventos da nota;
- não valida todo o XML contra o XSD oficial;
- não autentica nem autoriza o chamador;
- não configura CORS;
- não persiste o XML nem o PDF.

Em produção, publique o endpoint atrás dos controles de autenticação e rede já
adotados pela empresa. Para chamadas de navegador em outra origem, configure
CORS no proxy/servidor ou faça a chamada pelo backend.

O Base64 aumenta o tamanho do PDF em aproximadamente 33%. Confirme os limites
de requisição/resposta, timeout e memória do proxy, servidor e consumidor,
principalmente em processamento em lote.

## 10. Dependência do IBGE

O arquivo `src/main/resources/danfse/municipios-ibge.txt` é empacotado no WAR e
é a fonte principal para converter código IBGE em nome do município. Assim, a
geração normal não depende da internet.

Somente se esse recurso estiver ausente ou inválido o componente tenta a API
pública do IBGE. Não remova esse arquivo do empacotamento.

## 11. Logs e diagnóstico

Em sucesso, o componente registra no log padrão do servidor o número da NFS-e,
a chave de acesso e o tamanho do PDF. Exceções internas também são enviadas ao
log do servidor.

Para diagnóstico funcional:

1. Refaça a chamada com `debug=true`.
2. Localize o campo pelo `bloco` e `labelImpresso`.
3. Confira `caminhoXmlOrigem` e `regraFormatacaoAplicada`.
4. Verifique se algum indicador de traço, tabela ou frase especial foi ativado.
5. Consulte o log do servidor caso a resposta seja HTTP 500.

Como a chave da NFS-e aparece no log, aplique a política corporativa de acesso
e retenção de logs.

## 12. Problemas comuns

### HTTP 400 - corpo vazio

Confirme que o XML foi enviado diretamente no corpo e que o cliente não enviou
um objeto JSON vazio.

### HTTP 400 - raiz diferente de NFSe

O endpoint espera a NFS-e processada, com raiz `<NFSe>`, e não apenas a DPS.

### HTTP 500 ao ler o XML

Verifique encoding, caracteres inválidos, tags não fechadas e se o conteúdo foi
alterado por escape de JSON/HTML. Prefira envio binário/raw, como
`curl --data-binary`.

### PDF sem marca de cancelamento

O XML não determina sozinho o cancelamento. Envie explicitamente
`?situacao=CANCELADA` após confirmar o evento no sistema de origem.

### Município impresso com traço

Confirme o código IBGE no XML e a presença de
`danfse/municipios-ibge.txt` dentro do WAR.

### Funciona no backend, mas falha no navegador

Verifique autenticação, cookies, HTTPS, conteúdo misto e política CORS. O WAR
não adiciona cabeçalhos CORS automaticamente.

## 13. Checklist de entrega

- [ ] JDK 11 e Maven conferidos.
- [ ] `mvn clean package` finalizado com sucesso.
- [ ] Sete testes automatizados executados sem falha.
- [ ] `target/datawer.war` publicado no servidor correto.
- [ ] Contexto `/datawer` acessível pelo proxy.
- [ ] XML real de homologação gerou PDF válido.
- [ ] QR Code foi testado.
- [ ] Cenários normal, cancelado e substituído foram conferidos.
- [ ] Autenticação/restrição de rede foi aplicada externamente.
- [ ] Limites de payload e timeout foram conferidos.
- [ ] Chamada com `debug=true` foi validada em homologação.

## 14. Arquivos principais

| Arquivo | Responsabilidade |
| --- | --- |
| `pom.xml` | Dependências, Java 11 e empacotamento do WAR |
| `DanfseResource.java` | Contrato REST e serialização da resposta |
| `DanfseGenerator.java` | Regras, formatação e desenho do DANFSe |
| `NfseXml.java` | Leitura segura e navegação pelo XML |
| `IbgeMunicipios.java` | Conversão de código IBGE para município |
| `DanfseAuditEntry.java` | Estrutura de auditoria por campo |
| `DanfseGeneratorTest.java` | Testes automatizados de conformidade |
| `jboss-web.xml` | Context root `/datawer` |
