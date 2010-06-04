package org.broadinstitute.sting.gatk.contexts.variantcontext;

import org.broadinstitute.sting.utils.Utils;

import java.util.*;

/**
 * This class encompasses all the basic information about a genotype.  It is immutable.
 *
 * @author Mark DePristo
 */
public class Genotype {

    public final static String PHASED_ALLELE_SEPARATOR = "|";
    public final static String UNPHASED_ALLELE_SEPARATOR = "/";

    protected InferredGeneticContext commonInfo;
    public final static double NO_NEG_LOG_10PERROR = InferredGeneticContext.NO_NEG_LOG_10PERROR;
    protected List<Allele> alleles = null; // new ArrayList<Allele>();
    private boolean genotypesArePhased = false;

    public Genotype(String sampleName, List<Allele> alleles, double negLog10PError, Set<String> filters, Map<String, ?> attributes, boolean genotypesArePhased) {
        this.alleles = Collections.unmodifiableList(alleles);
        commonInfo = new InferredGeneticContext(sampleName, negLog10PError, filters, attributes);
        this.genotypesArePhased = genotypesArePhased;
        validate();
    }

    public Genotype(String sampleName, List<Allele> alleles, double negLog10PError) {
        this(sampleName, alleles, negLog10PError, null, null, false);
    }

    public Genotype(String sampleName, List<Allele> alleles) {
        this(sampleName, alleles, NO_NEG_LOG_10PERROR, null, null, false);
    }

    /**
     * @return the alleles for this genotype
     */
    public List<Allele> getAlleles() {
        return alleles;
    }

    public List<Allele> getAlleles(Allele allele) {
        List<Allele> al = new ArrayList<Allele>();
        for ( Allele a : alleles )
            if ( a.equals(allele) )
                al.add(a);

        return Collections.unmodifiableList(al);
    }

    public Allele getAllele(int i) {
        return alleles.get(i);
    }

    public boolean genotypesArePhased() { return genotypesArePhased; }

    /**
     * @return the ploidy of this genotype
     */
    public int getPloidy() { return alleles.size(); }

    public enum Type {
        NO_CALL,
        HOM_REF,
        HET,
        HOM_VAR
    }

    public Type getType() {
        Allele firstAllele = alleles.get(0);

        if ( firstAllele.isNoCall() ) {
            return Type.NO_CALL;
        }

        for (Allele a : alleles) {
            if ( ! firstAllele.equals(a) )
                return Type.HET;
        }
        return firstAllele.isReference() ? Type.HOM_REF : Type.HOM_VAR;
    }

    /**
     * @return true if all observed alleles are the same (regardless of whether they are ref or alt)
     */
    public boolean isHom()    { return isHomRef() || isHomVar(); }
    public boolean isHomRef() { return getType() == Type.HOM_REF; }
    public boolean isHomVar() { return getType() == Type.HOM_VAR; }
    
    /**
     * @return true if we're het (observed alleles differ)
     */
    public boolean isHet() { return getType() == Type.HET; }

    /**
     * @return true if this genotype is not actually a genotype but a "no call" (e.g. './.' in VCF)
     */
    public boolean isNoCall() { return getType() == Type.NO_CALL; }
    public boolean isCalled() { return getType() != Type.NO_CALL; }

    public void validate() {
        // todo -- add validation checking here

        if ( alleles == null ) throw new IllegalArgumentException("BUG: alleles cannot be null in setAlleles");
        if ( alleles.size() == 0) throw new IllegalArgumentException("BUG: alleles cannot be of size 0 in setAlleles");

        int nNoCalls = 0;
        for ( Allele allele : alleles ) {
            if ( allele == null )
                throw new IllegalArgumentException("BUG: allele cannot be null in Genotype");
            nNoCalls += allele.isNoCall() ? 1 : 0;
        }
        if ( nNoCalls > 0 && nNoCalls != alleles.size() )
            throw new IllegalArgumentException("BUG: alleles include some No Calls and some Calls, an illegal state " + this);
    }

    public String getGenotypeString() {
        return getGenotypeString(true);
    }

    public String getGenotypeString(boolean ignoreRefState) {
        // Notes:
        // 1. Make sure to use the appropriate separator depending on whether the genotype is phased
        // 2. If ignoreRefState is true, then we want just the bases of the Alleles (ignoring the '*' indicating a ref Allele)
        // 3. So that everything is deterministic with regards to integration tests, we sort Alleles (when the genotype isn't phased, of course)
        return Utils.join(genotypesArePhased() ? PHASED_ALLELE_SEPARATOR : UNPHASED_ALLELE_SEPARATOR,
                ignoreRefState ? getAlleleStrings() : (genotypesArePhased() ? getAlleles() : Utils.sorted(getAlleles())));
    }

    private List<String> getAlleleStrings() {
        List<String> al = new ArrayList<String>();
        for ( Allele a : alleles )
            al.add(new String(a.getBases()));

        return al;
    }

    public String toString() {
        return String.format("[GT: %s %s %s Q%.2f %s]", getSampleName(), getGenotypeString(false), getType(), getPhredScaledQual(), Utils.sortedString(getAttributes()));
    }

    public String toBriefString() {
        return String.format("%s:Q%.2f", getGenotypeString(false), getPhredScaledQual());
    }

    public boolean sameGenotype(Genotype other) {
        return sameGenotype(other, true);
    }

    public boolean sameGenotype(Genotype other, boolean ignorePhase) {
        if ( getPloidy() != other.getPloidy() )
            return false; // gotta have the same number of allele to be equal for gods sake

        // algorithms are wildly different if phase is kept of ignored
        if ( ignorePhase ) {
            for ( int i = 0; i < getPloidy(); i++) {
                Allele myAllele    = getAllele(i);
                Allele otherAllele = other.getAllele(i);
                if ( ! myAllele.basesMatch(otherAllele) )
                    return false;
            }
        } else {
            List<Allele> otherAlleles = new ArrayList<Allele>(other.getAlleles());
            for ( Allele myAllele : getAlleles() ) {
                Allele alleleToRemove = null;
                for ( Allele otherAllele : otherAlleles ) {
                    if ( myAllele.basesMatch(otherAllele) ) {
                        alleleToRemove = otherAllele;
                        break;
                    }
                }

                if ( alleleToRemove != null )
                    otherAlleles.remove(alleleToRemove);
                else
                    return false;   // we couldn't find our allele
            }
        }

        return true;
    }
    
    // ---------------------------------------------------------------------------------------------------------
    // 
    // get routines to access context info fields
    //
    // ---------------------------------------------------------------------------------------------------------
    public String getSampleName()       { return commonInfo.getName(); }
    public Set<String> getFilters()     { return commonInfo.getFilters(); }
    public boolean isFiltered()         { return commonInfo.isFiltered(); }
    public boolean isNotFiltered()      { return commonInfo.isNotFiltered(); }
    public boolean hasNegLog10PError()  { return commonInfo.hasNegLog10PError(); }
    public double getNegLog10PError()   { return commonInfo.getNegLog10PError(); }
    public double getPhredScaledQual()  { return commonInfo.getPhredScaledQual(); }

    public Map<String, Object> getAttributes()  { return commonInfo.getAttributes(); }
    public boolean hasAttribute(String key)     { return commonInfo.hasAttribute(key); }
    public Object getAttribute(String key)      { return commonInfo.getAttribute(key); }
    
    public Object getAttribute(String key, Object defaultValue) {
        return commonInfo.getAttribute(key, defaultValue); 
    }

    public String getAttributeAsString(String key)                        { return commonInfo.getAttributeAsString(key); }
    public String getAttributeAsString(String key, String defaultValue)   { return commonInfo.getAttributeAsString(key, defaultValue); }
    public int getAttributeAsInt(String key)                              { return commonInfo.getAttributeAsInt(key); }
    public int getAttributeAsInt(String key, int defaultValue)            { return commonInfo.getAttributeAsInt(key, defaultValue); }
    public double getAttributeAsDouble(String key)                        { return commonInfo.getAttributeAsDouble(key); }
    public double getAttributeAsDouble(String key, double  defaultValue)  { return commonInfo.getAttributeAsDouble(key, defaultValue); }
}