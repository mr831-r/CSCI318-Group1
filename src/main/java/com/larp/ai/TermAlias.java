package com.larp.ai;

import com.larp.database.IDatabaseItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A piece of terminology the agent has learned, e.g. "blade" means "sword" or "dagger".
 * Satisfies the requirement "Recognise and learn unfamiliar item terminology to improve future searches".
 *
 * Stored through the shared DatabaseService like every other IDatabaseItem, under the filter name "term-alias"
 * (so GET /database/term-alias lists everything learned).
 */
@Entity
@Table(name = "ai_term_aliases")
public class TermAlias implements IDatabaseItem {

    public static final String FILTER_NAME = "term-alias";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The unfamiliar word, stored lower case. Exposed as the item code so DatabaseService can look it up. */
    @Column(nullable = false)
    private String term;

    /** Space separated words the term should expand to when searching, stored lower case. */
    @Column(nullable = false)
    private String meaning;

    @Column(nullable = false)
    private LocalDate learnedOn;

    @Column
    private String learnedFrom;

    public TermAlias() {
    }

    public TermAlias(String term, String meaning, LocalDate learnedOn, String learnedFrom) {
        this.term = term;
        this.meaning = meaning;
        this.learnedOn = learnedOn;
        this.learnedFrom = learnedFrom;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getFilterName() {
        return FILTER_NAME;
    }

    @Override
    public String getItemCode() {
        return term;
    }

    @Override
    public LocalDate getDate() {
        return learnedOn;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public LocalDate getLearnedOn() {
        return learnedOn;
    }

    public void setLearnedOn(LocalDate learnedOn) {
        this.learnedOn = learnedOn;
    }

    public String getLearnedFrom() {
        return learnedFrom;
    }

    public void setLearnedFrom(String learnedFrom) {
        this.learnedFrom = learnedFrom;
    }

    @Override
    public void updateFrom(IDatabaseItem incoming) {
        if (!(incoming instanceof TermAlias alias)) {
            throw new IllegalArgumentException("Term aliases can only be updated from term aliases");
        }
        if (alias.getTerm() != null) {
            setTerm(alias.getTerm());
        }
        if (alias.getMeaning() != null) {
            setMeaning(alias.getMeaning());
        }
        if (alias.getLearnedOn() != null) {
            setLearnedOn(alias.getLearnedOn());
        }
        if (alias.getLearnedFrom() != null) {
            setLearnedFrom(alias.getLearnedFrom());
        }
    }
}
