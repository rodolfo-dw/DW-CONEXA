package br.com.datawer.danfse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Wrapper de leitura do XML da NFS-e Nacional (padrão SEFIN Nacional, versão 1.01).
 * Navegação por nome local de elemento, ignorando namespace, sempre a partir
 * dos filhos diretos - evita colisão entre tags repetidas (ex.: "valores"
 * existe em infNFSe, em IBSCBS e em infDPS).
 */
public class NfseXml {

	private final Element nfse;

	private NfseXml(Element nfse) {
		this.nfse = nfse;
	}

	public static NfseXml parse(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		// proteção contra XXE
		dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		dbf.setExpandEntityReferences(false);
		Document doc = dbf.newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		Element root = doc.getDocumentElement();
		if (!"NFSe".equals(root.getLocalName())) {
			throw new IllegalArgumentException("XML informado não é uma NFS-e (raiz: " + root.getLocalName() + ")");
		}
		return new NfseXml(root);
	}

	/** Elemento navegando por filhos diretos. Retorna null se algum nível não existir. */
	public Element el(String... path) {
		Element cur = nfse;
		for (String name : path) {
			cur = child(cur, name);
			if (cur == null) {
				return null;
			}
		}
		return cur;
	}

	/** Texto do elemento no caminho, ou "" se ausente. */
	public String txt(String... path) {
		Element e = el(path);
		return e == null ? "" : e.getTextContent().trim();
	}

	public boolean has(String... path) {
		return el(path) != null;
	}

	/** Chave de acesso (50 dígitos): atributo Id de infNFSe sem o prefixo "NFS". */
	public String chaveAcesso() {
		Element inf = el("infNFSe");
		String id = inf == null ? "" : inf.getAttribute("Id");
		return id.startsWith("NFS") ? id.substring(3) : id;
	}

	private static Element child(Element parent, String localName) {
		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() == Node.ELEMENT_NODE && localName.equals(n.getLocalName())) {
				return (Element) n;
			}
		}
		return null;
	}
}
