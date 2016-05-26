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

*/
package com.github.lindenb.jvarkit.tools.burden;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import htsjdk.samtools.util.CloserUtil;
import htsjdk.tribble.readers.LineIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;

import com.github.lindenb.jvarkit.io.IOUtils;
import com.github.lindenb.jvarkit.util.Pedigree;
import com.github.lindenb.jvarkit.util.picard.SAMSequenceDictionaryProgress;
import com.github.lindenb.jvarkit.util.vcf.VCFUtils;
import com.github.lindenb.jvarkit.util.vcf.VcfIterator;

/**
 *     
 * VcfBurdenRscriptV
 * @author lindenb
 *
 */
public class VcfBurdenRscriptV
	extends AbstractVcfBurdenRscriptV
	{
	private static final org.slf4j.Logger LOG = com.github.lindenb.jvarkit.util.log.Logging.getLog(VcfBurdenRscriptV.class);
	
	
	public VcfBurdenRscriptV()
		{
		}
	
	private static class Variant {
		String contig;
		int start;
		int end;
		Allele ref;
		Allele alt;
		Double maf=null;
		
	}
	
	@Override
	protected Collection<Throwable> call(final String inputName) throws Exception {
		/* 
		 mail matile April 11: requires calling R for SKAT: needs
		 matrix of samples and genotypes */
		PrintWriter pw = null;
		VcfIterator in = null;
		LineIterator lr = null;
		long vcf_id = System.currentTimeMillis();
		try {
			lr =(
				inputName==null?
				IOUtils.openStreamForLineIterator(stdin()):
				IOUtils.openURIForLineIterator(inputName)
				);
			
			pw = super.openFileOrStdoutAsPrintWriter();
			pw.println("# This is the header of the generated R script. ");
			pw.println("# generated by "+ getName());
			pw.println("# version "+getGitHash());
			

			
			if(super.userDefinedFunName!=null && !super.userDefinedFunName.trim().isEmpty()) {
				pw.println("# user defined function "+userDefinedFunName+" should have been defined BEFORE this header. ");
				pw.println("#  something like 'cat user_function.R this_file.R |  R --no-save > result.txt'");
			}
			
			
			
			if(!lr.hasNext()) {
				LOG.warn("No input found. Couldn't read any VCF header file");
			}
			
			while(lr.hasNext()) {
			vcf_id++;
			in = VCFUtils.createVcfIteratorFromLineIterator(lr,true);
			
			final VCFHeader header=in.getHeader();
			final Set<Pedigree.Person> samples = new TreeSet<>( super.getCasesControlsInPedigree(header));
			final List<Variant> variants = new ArrayList<>();

		

			
			boolean first=true;
			pw.println("# BEGIN  VCF ##########################################");
			pw.println("# Title ");
			final VCFHeaderLine vcfTitle= 
					(
					super.titleHeaderStr==null || super.titleHeaderStr.trim().isEmpty() ?
					null :
					header.getOtherHeaderLine(super.titleHeaderStr.trim())
					);
			if(vcfTitle==null) {
				pw.println("# [WARNING] No title was found. ");
				
				pw.println("vcf.title <- \"vcf"+String.format("%04d", vcf_id)+"\"");
			} else {
				pw.println("vcf.title <- \""+vcfTitle.getValue()+"\"");
			}
			
			
			first=true;
			pw.println("# samples ( 0: unaffected 1:affected)");
			
			pw.print("population <- data.frame(family=c(");			
			first=true;for(final Pedigree.Person person : samples) { if(!first) pw.print(","); pw.print("\""+person.getFamily().getId()+"\"");first=false;}
			pw.print("),name=c(");
			first=true;for(final Pedigree.Person person : samples) { if(!first) pw.print(","); pw.print("\""+person.getId()+"\"");first=false;}
			pw.print("),status=c(");
			first=true;for(final Pedigree.Person person : samples) { if(!first) pw.print(","); pw.print(person.isUnaffected()?0:1);first=false;}
			pw.println("))");
						
			
			first=true;
			pw.println();
			pw.println("# genotypes as a list. Should be a multiple of length(samples).");
			pw.println("# 0 is homref (0/0), 1 is het (0/1), 2 is homvar (1/1)");
			pw.println("# if the variant contains another ALT allele: (0/2) and (2/2) are considered 0 (homref)");
			pw.print("genotypes <- c(");
			final SAMSequenceDictionaryProgress progess=new SAMSequenceDictionaryProgress(header.getSequenceDictionary());
			while(in.hasNext()) {
				final VariantContext ctx = progess.watch(in.next());
				if(ctx.isFiltered() && !super.acceptFiltered) continue;
				final int n_alts = ctx.getAlternateAlleles().size();
				if( n_alts == 0) {
					LOG.warn("ignoring variant without ALT allele.");
					continue;
				}
				if( n_alts > 1) {
					LOG.warn("variant with more than one ALT. Using getAltAlleleWithHighestAlleleCount.");
				}
				final boolean is_chrom_X = (ctx.getContig().equals("X") ||  ctx.getContig().equals("chrX"));
				final Allele observed_alt = ctx.getAltAlleleWithHighestAlleleCount();
				double count_total = 0.0;
				double count_alt = 0.0;
				for (final Pedigree.Person person : samples) {
						final Genotype genotype = ctx.getGenotype(person.getId());
						if(genotype==null) {
							pw.close();pw=null;
							in.close();
							throw new IllegalStateException();
						}
						
						/* loop over alleles */
						for(final Allele a: genotype.getAlleles()) {
							/* chromosome X and male ? count half */
							if( is_chrom_X && person.isMale()) {
								count_total+=0.5;
								}
							else
								{
								count_total+=1.0;
								}
							if(a.equals(observed_alt))
								{
								count_alt++;
								}
							}
						
						
						
						if(!first) pw.print(",");
						if (genotype.isHomRef()) {
							pw.print('0');
						} else if (genotype.isHomVar() && genotype.getAlleles().contains(observed_alt)) {
							pw.print('2');
						} else if (genotype.isHet() && 
								genotype.getAlleles().contains(observed_alt) &&
								genotype.getAlleles().contains(ctx.getReference())) {
							pw.print('1');
						}
						/* we treat 0/2 has hom-ref */
						else if (genotype.isHet() && 
								!genotype.getAlleles().contains(observed_alt) &&
								genotype.getAlleles().contains(ctx.getReference())) {
							LOG.warn("Treating "+genotype+" as hom-ref (0) alt="+observed_alt);
							pw.print('0');
							
						} 
						/* we treat 2/2 has hom-ref */
						else if (genotype.isHomVar() && !genotype.getAlleles().contains(observed_alt)) {
							LOG.warn("Treating "+genotype+" as hom-ref (0) alt="+observed_alt);
							pw.print('0');
						}
						else {
							pw.print("-9");
						}
						first=false;
					}
				
				final Variant variant = new Variant();
				variant.contig = ctx.getContig();
				variant.start = ctx.getStart();
				variant.end = ctx.getEnd();
				variant.ref = ctx.getReference();
				variant.alt = observed_alt;
				if(count_total>0)
					{
					variant.maf =(double)count_alt/(double)count_total;
					}
				else
					{
					variant.maf = null;
					}
				variants.add(variant);
				}// end reading vcf
			progess.finish();
			in.close();
			
			pw.println(")");
			first = true;
			
			pw.println("# variants. CONTIG/START/END/REF/ALT/MAF");
			pw.print("variants <- data.frame(chrom=c(");
			first=true; for(final Variant v: variants) {if(!first) pw.print(",");pw.print("\""+v.contig+"\"");first=false;}
			pw.print("),chromStart=c(");
			first=true; for(final Variant v: variants) {if(!first) pw.print(",");pw.print(v.start);first=false;}
			pw.print("),chromEnd=c(");
			first=true; for(final Variant v: variants) {if(!first) pw.print(",");pw.print(v.end);first=false;}
			pw.print("),refAllele=c(");
			first=true; for(final Variant v: variants) {if(!first) pw.print(",");pw.print("\""+v.ref.getDisplayString()+"\"");first=false;}
			pw.print("),altAllele=c(");
			first=true; for(final Variant v: variants) {if(!first) pw.print(",");pw.print("\""+v.alt.getDisplayString()+"\"");first=false;}
			pw.print("),maf=c(");
			first=true; for(final Variant v: variants) {if(!first) pw.print(",");pw.print(v.maf==null?"NA":String.valueOf(v.maf));first=false;}
			pw.println("))");
			
			pw.println("# assert sizes");
			pw.println("stopifnot( length(genotypes) %% NROW(population) == 0 )");
			pw.println("stopifnot(NROW(variants) * NROW(population) == length(genotypes) )");
			
			
			if(super.userDefinedFunName==null || super.userDefinedFunName.trim().isEmpty()) {
				pw.println("## WARNING not user-defined R function was defined");
				}
			else
				{
				pw.println("# consumme data with user-defined R function ");
				pw.println(super.userDefinedFunName+"()");
				}
			
			pw.println("# END VCF ##########################################");
			
			}
			
			
			
			pw.flush();
			if(pw.checkError()) {
				return wrapException("I/O error");
			}
			pw.close();pw=null;
			
					
			
			
			LOG.info("done");
			return RETURN_OK;
			} catch(Exception err) {
				return wrapException(err);
			} finally {
				CloserUtil.close(pw);
				CloserUtil.close(in);
				CloserUtil.close(lr);
			}
		}
	
	public static void main(String[] args)
		{
		new VcfBurdenRscriptV().instanceMainWithExit(args);
		}
	}
