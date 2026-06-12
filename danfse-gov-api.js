(async function () {
    var retornoConsulta = {
        "StatusProcessamento": "DOCUMENTOS_LOCALIZADOS",
        "LoteDFe": [
            {
                "NSU": 42,
                "ChaveAcesso": "32052001242370540000107000000000013326010591989616",
                "TipoDocumento": "NFSE",
                "ArquivoXml": "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",
                "DataHoraGeracao": "2026-01-03T15:03:40.317"
            },
            {
                "NSU": 43,
                "ChaveAcesso": "32052001242370540000107000000000013326010591989616",
                "TipoDocumento": "EVENTO",
                "TipoEvento": "CANCELAMENTO",
                "ArquivoXml": "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",
                "DataHoraGeracao": "2026-01-03T15:04:09.28"
            }
        ],
        "Alertas": [],
        "Erros": [],
        "TipoAmbiente": "PRODUCAO",
        "VersaoAplicativo": "1.0.0.0",
        "DataHoraProcessamento": "2026-06-12T09:11:20.1279757-03:00"
    };

    var endpoint = '/datawer/api/v1/danfse/base64';
    var baixarPdfs = true;
    var baixarXmls = true;

    async function decodeArquivoXml(arquivoXmlBase64) {
        var binario = atob(arquivoXmlBase64);
        var bytes = new Uint8Array(binario.length);

        for (var i = 0; i < binario.length; i++) {
            bytes[i] = binario.charCodeAt(i);
        }

        if (typeof DecompressionStream !== 'undefined') {
            var stream = new Blob([bytes]).stream();
            var decompressedStream = stream.pipeThrough(new DecompressionStream('gzip'));
            return await new Response(decompressedStream).text();
        }

        if (typeof pako !== 'undefined' && pako.ungzip) {
            var xmlBytes = pako.ungzip(bytes);
            return new TextDecoder('utf-8').decode(xmlBytes);
        }

        throw new Error('Seu navegador nao suporta DecompressionStream. Use Chrome/Edge atualizado ou carregue a lib pako.');
    }

    function obterSituacaoPorChave(retorno, chaveAcesso) {
        var temCancelamento = retorno.LoteDFe.some(function (doc) {
            return (
                doc.ChaveAcesso === chaveAcesso &&
                doc.TipoDocumento === 'EVENTO' &&
                doc.TipoEvento === 'CANCELAMENTO'
            );
        });

        if (temCancelamento) {
            return 'CANCELADA';
        }

        return 'NORMAL';
    }

    function baixarXml(xml, fileName) {
        var url = URL.createObjectURL(new Blob([xml], { type: 'application/xml;charset=utf-8' }));
        var link = document.createElement('a');
        link.href = url;
        link.download = fileName || 'nfse.xml';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    }

    function baixarPdf(base64, fileName) {
        var byteChars = atob(base64);
        var bytes = new Uint8Array(byteChars.length);

        for (var i = 0; i < byteChars.length; i++) {
            bytes[i] = byteChars.charCodeAt(i);
        }

        var url = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
        var link = document.createElement('a');
        link.href = url;
        link.download = fileName || 'danfse.pdf';
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
    }

    async function chamarApi(body, query, contentType) {
        var response = await fetch(endpoint + (query || ''), {
            method: 'POST',
            headers: {
                'Content-Type': contentType || 'application/xml',
                'Accept': 'application/json'
            },
            body: body
        });

        var texto = await response.text();
        var result;

        try {
            result = JSON.parse(texto);
        } catch (erro) {
            throw new Error('Resposta HTTP ' + response.status + ' nao e JSON: ' + texto.substring(0, 200));
        }

        return { response: response, result: result };
    }

    function validarPdf(chamada, situacaoEsperada) {
        var response = chamada.response;
        var result = chamada.result;

        if (!response.ok || !result.success) {
            throw new Error(result.message || ('HTTP ' + response.status));
        }

        if (response.status !== 200) {
            throw new Error('HTTP esperado 200, recebido ' + response.status);
        }

        if (result.situacao !== situacaoEsperada) {
            throw new Error('Situacao esperada ' + situacaoEsperada + ', recebida ' + result.situacao);
        }

        if (result.contentType !== 'application/pdf') {
            throw new Error('Content-Type do arquivo inesperado: ' + result.contentType);
        }

        if (!result.pdfBase64 || result.pdfBase64.indexOf('JVBERi0') !== 0) {
            throw new Error('pdfBase64 ausente ou sem assinatura de PDF');
        }

        if (!Array.isArray(result.audit) || result.audit.length === 0) {
            throw new Error('debug=true nao retornou auditoria');
        }

        return result;
    }

    function nomePdfPorCenario(fileName, situacao) {
        var nome = fileName || 'DANFSe.pdf';
        return nome.replace(/\.pdf$/i, '') + '-' + situacao + '.pdf';
    }

    var notas = [];

    for (var indiceDoc = 0; indiceDoc < retornoConsulta.LoteDFe.length; indiceDoc++) {
        var doc = retornoConsulta.LoteDFe[indiceDoc];

        if (doc.TipoDocumento !== 'NFSE') {
            continue;
        }

        var xmlNfse = await decodeArquivoXml(doc.ArquivoXml);
        var situacao = obterSituacaoPorChave(retornoConsulta, doc.ChaveAcesso);

        notas.push({
            nome: doc.ChaveAcesso + '.xml',
            chaveAcesso: doc.ChaveAcesso,
            nsu: doc.NSU,
            xml: xmlNfse,
            situacao: situacao
        });

        console.log('XML DECODIFICADO DA NFSe - NSU ' + doc.NSU);
        console.log(xmlNfse);
        console.log('SITUACAO DETECTADA:', situacao);

        if (baixarXmls) {
            baixarXml(xmlNfse, doc.ChaveAcesso + '.xml');
        }
    }

    if (notas.length === 0) {
        throw new Error('Nenhum documento TipoDocumento=NFSE encontrado no retorno.');
    }

    var resultadosPdf = [];
    var totalPdf = notas.length;
    var atualPdf = 0;

    for (var indice = 0; indice < notas.length; indice++) {
        var nota = notas[indice];
        atualPdf++;

        var query = '?debug=true';

        if (nota.situacao && nota.situacao !== 'NORMAL') {
            query = '?situacao=' + encodeURIComponent(nota.situacao) + '&debug=true';
        }

        console.log('[' + atualPdf + '/' + totalPdf + '] ' + nota.nome + ' - ' + nota.situacao);
        console.log('QUERY USADA:', query);

        try {
            var chamada = await chamarApi(nota.xml, query, 'application/xml');
            var result = validarPdf(chamada, nota.situacao);
            var nomePdf = nomePdfPorCenario(result.fileName, nota.situacao);

            resultadosPdf.push({
                arquivo: nota.nome,
                chaveAcesso: nota.chaveAcesso,
                nsu: nota.nsu,
                situacao: nota.situacao,
                status: 'OK',
                http: chamada.response.status,
                numeroNfse: result.numeroNfse,
                auditoria: result.audit.length,
                pdf: nomePdf
            });

            if (baixarPdfs) {
                baixarPdf(result.pdfBase64, nomePdf);
            }
        } catch (erro) {
            resultadosPdf.push({
                arquivo: nota.nome,
                chaveAcesso: nota.chaveAcesso,
                nsu: nota.nsu,
                situacao: nota.situacao,
                status: 'ERRO',
                mensagem: erro.message
            });

            console.error('ERRO em ' + nota.nome + ' [' + nota.situacao + ']:', erro);
        }
    }

    console.log('RESULTADOS DOS PDFs');
    console.table(resultadosPdf);

    var falhasPdf = resultadosPdf.filter(function (item) {
        return item.status !== 'OK';
    }).length;

    console.log('Concluido: ' + resultadosPdf.length + ' PDF(s). Falhas: ' + falhasPdf + '.');
})();