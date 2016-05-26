/*
The MIT License (MIT)

Copyright (c) 2014 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


History:
* 2014 creation

*/
package com.github.lindenb.jvarkit.tools.pubmed;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import com.github.lindenb.jvarkit.io.IOUtils;

import htsjdk.samtools.util.CloserUtil;

/**
 * PubmedMap
 *
 */
public class PubmedMap
	extends AbstractPubmedMap
	{
	private static final org.slf4j.Logger LOG = com.github.lindenb.jvarkit.util.log.Logging.getLog(PubmedMap.class);
	
	private static class Country {
	final String suffix;
	final String name;
	Country (String suffix,String name ) {
		this.suffix=suffix;
		this.name=name;
		}
	}
	
	private static Country build(final String suffix,final String name ) {
	return new Country(suffix,name);
	}
		
	
	static private final Country Countries[]=new Country[]
		{
		build("ac","Ascension Island"),
		build("ad","Andorra"),
		build("ae","United Arab Emirates"),
		build("af","Afghanistan"),
		build("ag","Antigua and Barbuda"),
		build("ai","Anguilla"),
		build("al","Albania"),
		build("am","Armenia"),
		build("an","Netherlands Antilles"),
		build("ao","Angola"),
		build("aq","Antarctica"),
		build("ar","Argentina"),
		build("as","American Samoa"),
		build("at","Austria"),
		build("au","Australia"),
		build("aw","Aruba"),
		build("ax","Aland Islands"),
		build("az","Azerbaijan"),
		build("ba","Bosnia and Herzegovina"),
		build("bb","Barbados"),
		build("bd","Bangladesh"),
		build("be","Belgium"),
		build("bf","Burkina Faso"),
		build("bg","Bulgaria"),
		build("bh","Bahrain"),
		build("bi","Burundi"),
		build("bj","Benin"),
		build("bm","Bermuda"),
		build("bn","Brunei Darussalam"),
		build("bo","Bolivia"),
		build("br","Brazil"),
		build("bs","Bahamas"),
		build("bt","Bhutan"),
		build("bv","Bouvet Island"),
		build("bw","Botswana"),
		build("by","Belarus"),
		build("bz","Belize"),
		build("ca","Canada"),
		build("cc","Cocos (Keeling) Islands"),
		build("cd","Congo, The Democratic Republic of the"),
		build("cf","Central African Republic"),
		build("cg","Congo, Republic of"),
		build("ch","Switzerland"),
		build("ci","Cote d'Ivoire"),
		build("ck","Cook Islands"),
		build("cl","Chile"),
		build("cm","Cameroon"),
		build("cn","China"),
		build("co","Colombia"),
		build("cr","Costa Rica"),
		build("cu","Cuba"),
		build("cv","Cape Verde"),
		build("cx","Christmas Island"),
		build("cy","Cyprus"),
		build("cz","Czech Republic"),
		build("de","Germany"),
		build("dj","Djibouti"),
		build("dk","Denmark"),
		build("dm","Dominica"),
		build("do","Dominican Republic"),
		build("dz","Algeria"),
		build("ec","Ecuador"),
		build("ee","Estonia"),
		build("eg","Egypt"),
		build("eh","Western Sahara"),
		build("er","Eritrea"),
		build("es","Spain"),
		build("et","Ethiopia"),
		build("eu","European Union"),
		build("fi","Finland"),
		build("fj","Fiji"),
		build("fk","Falkland Islands (Malvinas)"),
		build("fm","Micronesia, Federated States of"),
		build("fo","Faroe Islands"),
		build("fr","France"),
		build("ga","Gabon"),
		build("gb","United Kingdom"),
		build("gd","Grenada"),
		
		build("gf","French Guiana"),
		build("gg","Guernsey"),
		build("gh","Ghana"),
		build("gi","Gibraltar"),
		build("gl","Greenland"),
		build("gm","Gambia"),
		build("gn","Guinea"),
		build("gp","Guadeloupe"),
		build("gq","Equatorial Guinea"),
		build("gr","Greece"),
		build("gs","South Georgia and the South Sandwich Islands"),
		build("gt","Guatemala"),
		build("gu","Guam"),
		build("gw","Guinea-Bissau"),
		build("gy","Guyana"),
		build("hk","Hong Kong"),
		build("hm","Heard and McDonald Islands"),
		build("hn","Honduras"),
		build("hr","Croatia/Hrvatska"),
		build("ht","Haiti"),
		build("hu","Hungary"),
		build("id","Indonesia"),
		build("ie","Ireland"),
		build("il","Israel"),
		build("im","Isle of Man"),
		build("in","India"),
		build("io","British Indian Ocean Territory"),
		build("iq","Iraq"),
		build("ir","Iran, Islamic Republic of"),
		build("ir","Iran"),
		build("is","Iceland"),
		build("it","Italy"),
		build("je","Jersey"),
		build("jm","Jamaica"),
		build("jo","Jordan"),
		build("jp","Japan"),
		build("ke","Kenya"),
		build("kg","Kyrgyzstan"),
		build("kh","Cambodia"),
		build("ki","Kiribati"),
		build("km","Comoros"),
		build("kn","Saint Kitts and Nevis"),
		build("kp","Korea, Democratic People's Republic"),
		build("kr","Korea, Republic of"),
		build("kr","South Korea"),
		
		build("kw","Kuwait"),
		build("ky","Cayman Islands"),
		build("kz","Kazakhstan"),
		build("la","Lao People's Democratic Republic"),
		build("lb","Lebanon"),
		build("lc","Saint Lucia"),
		build("li","Liechtenstein"),
		build("lk","Sri Lanka"),
		build("lr","Liberia"),
		build("ls","Lesotho"),
		build("lt","Lithuania"),
		build("lu","Luxembourg"),
		build("lv","Latvia"),
		build("ly","Libyan Arab Jamahiriya"),
		build("ly","Libya"),
		build("ma","Morocco"),
		build("mc","Monaco"),
		build("md","Moldova, Republic of"),
		build("me","Montenegro"),
		build("mg","Madagascar"),
		build("mh","Marshall Islands"),
		build("mk","Macedonia, The Former Yugoslav Republic of"),
		build("ml","Mali"),
		build("mm","Myanmar"),
		build("mn","Mongolia"),
		build("mo","Macao"),
		build("mp","Northern Mariana Islands"),
		build("mq","Martinique"),
		build("mr","Mauritania"),
		build("ms","Montserrat"),
		build("mt","Malta"),
		build("mu","Mauritius"),
		build("mv","Maldives"),
		build("mw","Malawi"),
		build("mx","Mexico"),
		build("mx","méxico"),
		build("my","Malaysia"),
		build("mz","Mozambique"),
		build("na","Namibia"),
		build("nc","New Caledonia"),
		build("ne","Niger"),
		build("nf","Norfolk Island"),
		build("ng","Nigeria"),
		build("ni","Nicaragua"),
		build("nl","Netherlands"),
		build("no","Norway"),
		build("np","Nepal"),
		build("nr","Nauru"),
		build("nu","Niue"),
		build("nz","New Zealand"),
		build("om","Oman"),
		build("pa","Panama"),
		build("pe","Peru"),
		build("pf","French Polynesia"),
		build("pg","Papua New Guinea"),
		build("ph","Philippines"),
		build("pk","Pakistan"),
		build("pl","Poland"),
		build("pm","Saint Pierre and Miquelon"),
		build("pn","Pitcairn Island"),
		build("pr","Puerto Rico"),
		build("ps","Palestinian Territory, Occupied"),
		build("pt","Portugal"),
		build("pw","Palau"),
		build("py","Paraguay"),
		build("qa","Qatar"),
		build("re","Reunion Island"),
		build("ro","Romania"),
		build("rs","Serbia"),
		build("ru","Russian Federation"),
		
		build("rw","Rwanda"),
		build("sa","Saudi Arabia"),
		build("sb","Solomon Islands"),
		build("sc","Seychelles"),
		build("sd","Sudan"),
		build("se","Sweden"),
		build("sg","Singapore"),
		build("sh","Saint Helena"),
		build("si","Slovenia"),
		build("sj","Svalbard and Jan Mayen Islands"),
		build("sk","Slovak Republic"),
		build("sl","Sierra Leone"),
		build("sm","San Marino"),
		build("sn","Senegal"),
		build("so","Somalia"),
		build("sr","Suriname"),
		build("st","Sao Tome and Principe"),
		build("su","Soviet Union (being phased out)"),
		build("sv","El Salvador"),
		build("sy","Syrian Arab Republic"),
		build("sz","Swaziland"),
		build("tc","Turks and Caicos Islands"),
		build("td","Chad"),
		build("tf","French Southern Territories"),
		build("tg","Togo"),
		build("th","Thailand"),
		build("tj","Tajikistan"),
		build("tk","Tokelau"),
		build("tl","Timor-Leste"),
		build("tm","Turkmenistan"),
		build("tn","Tunisia"),
		build("to","Tonga"),
		build("tp","East Timor"),
		build("tr","Turkey"),
		build("tt","Trinidad and Tobago"),
		build("tv","Tuvalu"),
		build("tw","Taiwan"),
		build("tz","Tanzania"),
		build("ua","Ukraine"),
		build("ug","Uganda"),
		build("uk","United Kingdom"),
		
		build("um","United States Minor Outlying Islands"),
		build("us","United States"),
		build("gov","United States"),
		build("uy","Uruguay"),
		build("uz","Uzbekistan"),
		build("va","Holy See (Vatican City State)"),
		build("va","Vatican"),
		build("vc","Saint Vincent and the Grenadines"),
		build("ve","Venezuela"),
		build("vg","Virgin Islands, British"),
		build("vi","Virgin Islands, U.S."),
		build("vn","Vietnam"),
		build("vu","Vanuatu"),
		build("wf","Wallis and Futuna Islands"),
		build("ws","Samoa"),
		build("ye","Yemen"),
		build("yt","Mayotte"),
		build("yu","Yugoslavia"),
		build("za","South Africa"),
		build("zm","Zambia"),
		build("zw","Zimbabwe"),
		
		build("ge","Georgia"),
		build("uk"," UK."),
		build("uk"," UK,"),
		build("uk"," UK,"),
		build("us"," england."),
		build("us"," u.s.a."),
		build("us"," usa,"),
		build("us"," usa."),
		build("us",",usa,"),
		build("us"," new york"),
		build("ru","Russia"),
		build("br","Brasil"),
		build("es","españa"),
		build("us","stanford"),
		build("us","cornell"),
		build("us","san-francisco"),
		build("us","san francisco"),
		build("us","calfornia"),
		build("us","boston"),build("us","atlanta"),build("us","chicago"),
		build("fr","cedex"),
		build("kr","Korea")
		};
		
	
	public PubmedMap()
		{
		}
	
	private Country decodeAffiliation(final String affiliation) {
		if(affiliation==null || affiliation.isEmpty()) return null;
		final String Affiliation=affiliation.
				replaceAll("\\[[0-9]+\\]", "").
				replaceAll("\\([ ]*at[ ]*\\)", "@").
				trim();
		final List<String> tokens = new ArrayList<>(Arrays.asList( Affiliation.split("[ \t\\:\\<,\\>\\(\\)]")));
		
		Collections.reverse(tokens);//interesting information is usually at the end
		
		for(String mail:tokens)
				{							
				mail = mail.replaceAll("\\{\\}", "");
				if(mail.endsWith(".")) mail= mail.substring(0,mail.length()-1);
				int index=mail.indexOf('@');
				if(index==-1) continue;
				int i= mail.lastIndexOf('.');
				if(i==-1) continue;
				final String suffix= mail.substring(i+1);
				for(final Country country:Countries)
					{
					if(suffix.equals(country.suffix))
						{
						return country;
						}
					}
				}
			
			for(final Country country:Countries)
				{
				if(country.name.contains(" ") && affiliation.contains(country.name)) {
					return country;
				}
				
				for(String token: tokens) //ok starting from end because list is reversed
					{
					if(token.endsWith(".")) token= token.substring(0,token.length()-1);
					if(token.equals(country.name)) return country;
					}
				}
				
			LOG.info("Cannot find country for "+affiliation+" "+Affiliation);
			return null;
			}
	
	@Override
	protected Collection<Throwable> call(String inputName) throws Exception {
		OutputStream out=null;
		XMLEventReader r=null;
		InputStream in=null;
		XMLEventWriter w=null;
		try {
			final QName attDomainSuffix=new QName("domain");
			final QName attPlaceSuffix=new QName("place");
			final XMLEventFactory xmlEventFactory = XMLEventFactory.newFactory();
			final XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
			xmlInputFactory.setXMLResolver(new XMLResolver() {
				@Override
				public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace)
						throws XMLStreamException {
					LOG.debug("Ignoring resolve Entity");
					return new ByteArrayInputStream(new byte[0]);
				}
			});
			in=(inputName==null?stdin():IOUtils.openURIForReading(inputName));
			r = xmlInputFactory.createXMLEventReader(in);
			
			out = super.openFileOrStdoutAsStream();
			final XMLOutputFactory xof = XMLOutputFactory.newFactory();
			w=xof.createXMLEventWriter(out, "UTF-8");
			while(r.hasNext()) {
			final XMLEvent evt = r.nextEvent();
			if(evt.isStartElement() &&
					evt.asStartElement().getName().getLocalPart().equals("Affiliation") &&
					r.peek().isCharacters()
					)
				{
				final List<Attribute> attributes = new ArrayList<>();
				Iterator<?> t=evt.asStartElement().getAttributes();
				while(t.hasNext()) {
				    final Attribute att =  (Attribute)t.next();
				    if(att.getName().equals(attDomainSuffix)) continue;
				    if(att.getName().equals(attPlaceSuffix)) continue;
					attributes.add(att);
				}
				
				
				final XMLEvent textEvt = r.nextEvent();
				final String affiliation= textEvt.asCharacters().getData();	
				final Country country = decodeAffiliation(affiliation);
				
				if(country!=null) {
					attributes.add(xmlEventFactory.createAttribute(attDomainSuffix, country.suffix));
					attributes.add(xmlEventFactory.createAttribute(attPlaceSuffix, country.name));
					}
				w.add(xmlEventFactory.createStartElement(
						evt.asStartElement().getName(),
						attributes.iterator(),
						evt.asStartElement().getNamespaces()
						));
				w.add(textEvt);
				continue;
				}
				
	
			w.add(evt);
			}
			
			r.close();r=null;
			in.close();in=null;
			w.flush();w.close();w=null;
			out.flush();out.close();out=null;
			return RETURN_OK;
		} catch (Exception e) {
			return wrapException(e);
		} finally {
			CloserUtil.close(r);
			CloserUtil.close(in);
			CloserUtil.close(w);
			CloserUtil.close(out);
		}

		}
		
	
	public static void main(String[] args) {
		new PubmedMap().instanceMainWithExit(args);
	}
}
