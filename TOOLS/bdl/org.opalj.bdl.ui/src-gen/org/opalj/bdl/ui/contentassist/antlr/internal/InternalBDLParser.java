package org.opalj.bdl.ui.contentassist.antlr.internal; 

import java.io.InputStream;
import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.*;
import org.eclipse.xtext.parser.impl.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
import org.eclipse.xtext.ui.editor.contentassist.antlr.internal.AbstractInternalContentAssistParser;
import org.eclipse.xtext.ui.editor.contentassist.antlr.internal.DFA;
import org.opalj.bdl.services.BDLGrammarAccess;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("all")
public class InternalBDLParser extends AbstractInternalContentAssistParser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "RULE_ID", "RULE_STRING", "RULE_INT", "RULE_ML_COMMENT", "RULE_SL_COMMENT", "RULE_WS", "RULE_ANY_OTHER", "'Parameters'", "':'", "'/'", "'\\\\'", "','", "'.'", "'='", "'DeadEdgesAnalysis'", "'UnusedLocalVariables'", "'GuardedAndUnguardedAccessAnalysis'", "'UnusedMethodsAnalysis'", "'UselessComputationsAnalysis'", "'constant computation'", "'dead path'", "'throws exception'", "'unguarded use'", "'unused'", "'useless'", "'Analysis of '", "';'", "'{'", "'}'", "'[suppress='", "']'", "'$'", "'Issues'", "'Categories:'", "'Kinds:'", "'Relevance:'", "'Package:'", "'Class:'", "'bug'", "'smell'", "'performance'", "'comprehensibility'"
    };
    public static final int T__19=19;
    public static final int T__15=15;
    public static final int T__16=16;
    public static final int T__17=17;
    public static final int T__18=18;
    public static final int T__11=11;
    public static final int T__12=12;
    public static final int T__13=13;
    public static final int T__14=14;
    public static final int RULE_ID=4;
    public static final int T__26=26;
    public static final int T__27=27;
    public static final int T__28=28;
    public static final int RULE_INT=6;
    public static final int T__29=29;
    public static final int T__22=22;
    public static final int RULE_ML_COMMENT=7;
    public static final int T__23=23;
    public static final int T__24=24;
    public static final int T__25=25;
    public static final int T__20=20;
    public static final int T__21=21;
    public static final int RULE_STRING=5;
    public static final int RULE_SL_COMMENT=8;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int EOF=-1;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int RULE_WS=9;
    public static final int RULE_ANY_OTHER=10;
    public static final int T__44=44;
    public static final int T__45=45;
    public static final int T__40=40;
    public static final int T__41=41;
    public static final int T__42=42;
    public static final int T__43=43;

    // delegates
    // delegators


        public InternalBDLParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public InternalBDLParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return InternalBDLParser.tokenNames; }
    public String getGrammarFileName() { return "../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g"; }


     
     	private BDLGrammarAccess grammarAccess;
     	
        public void setGrammarAccess(BDLGrammarAccess grammarAccess) {
        	this.grammarAccess = grammarAccess;
        }
        
        @Override
        protected Grammar getGrammar() {
        	return grammarAccess.getGrammar();
        }
        
        @Override
        protected String getValueForTokenName(String tokenName) {
        	return tokenName;
        }




    // $ANTLR start "entryRuleModel"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:60:1: entryRuleModel : ruleModel EOF ;
    public final void entryRuleModel() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:61:1: ( ruleModel EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:62:1: ruleModel EOF
            {
             before(grammarAccess.getModelRule()); 
            pushFollow(FOLLOW_ruleModel_in_entryRuleModel61);
            ruleModel();

            state._fsp--;

             after(grammarAccess.getModelRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleModel68); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleModel"


    // $ANTLR start "ruleModel"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:69:1: ruleModel : ( ruleModelContainer ) ;
    public final void ruleModel() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:73:2: ( ( ruleModelContainer ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:74:1: ( ruleModelContainer )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:74:1: ( ruleModelContainer )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:75:1: ruleModelContainer
            {
             before(grammarAccess.getModelAccess().getModelContainerParserRuleCall()); 
            pushFollow(FOLLOW_ruleModelContainer_in_ruleModel94);
            ruleModelContainer();

            state._fsp--;

             after(grammarAccess.getModelAccess().getModelContainerParserRuleCall()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleModel"


    // $ANTLR start "entryRuleModelContainer"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:88:1: entryRuleModelContainer : ruleModelContainer EOF ;
    public final void entryRuleModelContainer() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:89:1: ( ruleModelContainer EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:90:1: ruleModelContainer EOF
            {
             before(grammarAccess.getModelContainerRule()); 
            pushFollow(FOLLOW_ruleModelContainer_in_entryRuleModelContainer120);
            ruleModelContainer();

            state._fsp--;

             after(grammarAccess.getModelContainerRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleModelContainer127); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleModelContainer"


    // $ANTLR start "ruleModelContainer"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:97:1: ruleModelContainer : ( ( rule__ModelContainer__Group__0 ) ) ;
    public final void ruleModelContainer() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:101:2: ( ( ( rule__ModelContainer__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:102:1: ( ( rule__ModelContainer__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:102:1: ( ( rule__ModelContainer__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:103:1: ( rule__ModelContainer__Group__0 )
            {
             before(grammarAccess.getModelContainerAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:104:1: ( rule__ModelContainer__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:104:2: rule__ModelContainer__Group__0
            {
            pushFollow(FOLLOW_rule__ModelContainer__Group__0_in_ruleModelContainer153);
            rule__ModelContainer__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getModelContainerAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleModelContainer"


    // $ANTLR start "entryRuleAnalysisElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:116:1: entryRuleAnalysisElement : ruleAnalysisElement EOF ;
    public final void entryRuleAnalysisElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:117:1: ( ruleAnalysisElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:118:1: ruleAnalysisElement EOF
            {
             before(grammarAccess.getAnalysisElementRule()); 
            pushFollow(FOLLOW_ruleAnalysisElement_in_entryRuleAnalysisElement180);
            ruleAnalysisElement();

            state._fsp--;

             after(grammarAccess.getAnalysisElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleAnalysisElement187); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleAnalysisElement"


    // $ANTLR start "ruleAnalysisElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:125:1: ruleAnalysisElement : ( ( rule__AnalysisElement__Group__0 ) ) ;
    public final void ruleAnalysisElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:129:2: ( ( ( rule__AnalysisElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:130:1: ( ( rule__AnalysisElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:130:1: ( ( rule__AnalysisElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:131:1: ( rule__AnalysisElement__Group__0 )
            {
             before(grammarAccess.getAnalysisElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:132:1: ( rule__AnalysisElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:132:2: rule__AnalysisElement__Group__0
            {
            pushFollow(FOLLOW_rule__AnalysisElement__Group__0_in_ruleAnalysisElement213);
            rule__AnalysisElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getAnalysisElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleAnalysisElement"


    // $ANTLR start "entryRuleParameterContainer"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:144:1: entryRuleParameterContainer : ruleParameterContainer EOF ;
    public final void entryRuleParameterContainer() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:145:1: ( ruleParameterContainer EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:146:1: ruleParameterContainer EOF
            {
             before(grammarAccess.getParameterContainerRule()); 
            pushFollow(FOLLOW_ruleParameterContainer_in_entryRuleParameterContainer240);
            ruleParameterContainer();

            state._fsp--;

             after(grammarAccess.getParameterContainerRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterContainer247); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleParameterContainer"


    // $ANTLR start "ruleParameterContainer"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:153:1: ruleParameterContainer : ( ( rule__ParameterContainer__Group__0 ) ) ;
    public final void ruleParameterContainer() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:157:2: ( ( ( rule__ParameterContainer__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:158:1: ( ( rule__ParameterContainer__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:158:1: ( ( rule__ParameterContainer__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:159:1: ( rule__ParameterContainer__Group__0 )
            {
             before(grammarAccess.getParameterContainerAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:160:1: ( rule__ParameterContainer__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:160:2: rule__ParameterContainer__Group__0
            {
            pushFollow(FOLLOW_rule__ParameterContainer__Group__0_in_ruleParameterContainer273);
            rule__ParameterContainer__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getParameterContainerAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleParameterContainer"


    // $ANTLR start "entryRuleParametersElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:172:1: entryRuleParametersElement : ruleParametersElement EOF ;
    public final void entryRuleParametersElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:173:1: ( ruleParametersElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:174:1: ruleParametersElement EOF
            {
             before(grammarAccess.getParametersElementRule()); 
            pushFollow(FOLLOW_ruleParametersElement_in_entryRuleParametersElement300);
            ruleParametersElement();

            state._fsp--;

             after(grammarAccess.getParametersElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParametersElement307); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleParametersElement"


    // $ANTLR start "ruleParametersElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:181:1: ruleParametersElement : ( 'Parameters' ) ;
    public final void ruleParametersElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:185:2: ( ( 'Parameters' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:186:1: ( 'Parameters' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:186:1: ( 'Parameters' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:187:1: 'Parameters'
            {
             before(grammarAccess.getParametersElementAccess().getParametersKeyword()); 
            match(input,11,FOLLOW_11_in_ruleParametersElement334); 
             after(grammarAccess.getParametersElementAccess().getParametersKeyword()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleParametersElement"


    // $ANTLR start "entryRuleParameterElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:202:1: entryRuleParameterElement : ruleParameterElement EOF ;
    public final void entryRuleParameterElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:203:1: ( ruleParameterElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:204:1: ruleParameterElement EOF
            {
             before(grammarAccess.getParameterElementRule()); 
            pushFollow(FOLLOW_ruleParameterElement_in_entryRuleParameterElement362);
            ruleParameterElement();

            state._fsp--;

             after(grammarAccess.getParameterElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterElement369); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleParameterElement"


    // $ANTLR start "ruleParameterElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:211:1: ruleParameterElement : ( ( rule__ParameterElement__Alternatives ) ) ;
    public final void ruleParameterElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:215:2: ( ( ( rule__ParameterElement__Alternatives ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:216:1: ( ( rule__ParameterElement__Alternatives ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:216:1: ( ( rule__ParameterElement__Alternatives ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:217:1: ( rule__ParameterElement__Alternatives )
            {
             before(grammarAccess.getParameterElementAccess().getAlternatives()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:218:1: ( rule__ParameterElement__Alternatives )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:218:2: rule__ParameterElement__Alternatives
            {
            pushFollow(FOLLOW_rule__ParameterElement__Alternatives_in_ruleParameterElement395);
            rule__ParameterElement__Alternatives();

            state._fsp--;


            }

             after(grammarAccess.getParameterElementAccess().getAlternatives()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleParameterElement"


    // $ANTLR start "entryRuleParameterKeyValueElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:230:1: entryRuleParameterKeyValueElement : ruleParameterKeyValueElement EOF ;
    public final void entryRuleParameterKeyValueElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:231:1: ( ruleParameterKeyValueElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:232:1: ruleParameterKeyValueElement EOF
            {
             before(grammarAccess.getParameterKeyValueElementRule()); 
            pushFollow(FOLLOW_ruleParameterKeyValueElement_in_entryRuleParameterKeyValueElement422);
            ruleParameterKeyValueElement();

            state._fsp--;

             after(grammarAccess.getParameterKeyValueElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterKeyValueElement429); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleParameterKeyValueElement"


    // $ANTLR start "ruleParameterKeyValueElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:239:1: ruleParameterKeyValueElement : ( ( rule__ParameterKeyValueElement__Group__0 ) ) ;
    public final void ruleParameterKeyValueElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:243:2: ( ( ( rule__ParameterKeyValueElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:244:1: ( ( rule__ParameterKeyValueElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:244:1: ( ( rule__ParameterKeyValueElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:245:1: ( rule__ParameterKeyValueElement__Group__0 )
            {
             before(grammarAccess.getParameterKeyValueElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:246:1: ( rule__ParameterKeyValueElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:246:2: rule__ParameterKeyValueElement__Group__0
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Group__0_in_ruleParameterKeyValueElement455);
            rule__ParameterKeyValueElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getParameterKeyValueElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleParameterKeyValueElement"


    // $ANTLR start "entryRuleParameterKeyElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:258:1: entryRuleParameterKeyElement : ruleParameterKeyElement EOF ;
    public final void entryRuleParameterKeyElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:259:1: ( ruleParameterKeyElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:260:1: ruleParameterKeyElement EOF
            {
             before(grammarAccess.getParameterKeyElementRule()); 
            pushFollow(FOLLOW_ruleParameterKeyElement_in_entryRuleParameterKeyElement482);
            ruleParameterKeyElement();

            state._fsp--;

             after(grammarAccess.getParameterKeyElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterKeyElement489); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleParameterKeyElement"


    // $ANTLR start "ruleParameterKeyElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:267:1: ruleParameterKeyElement : ( ( rule__ParameterKeyElement__Group__0 ) ) ;
    public final void ruleParameterKeyElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:271:2: ( ( ( rule__ParameterKeyElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:272:1: ( ( rule__ParameterKeyElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:272:1: ( ( rule__ParameterKeyElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:273:1: ( rule__ParameterKeyElement__Group__0 )
            {
             before(grammarAccess.getParameterKeyElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:274:1: ( rule__ParameterKeyElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:274:2: rule__ParameterKeyElement__Group__0
            {
            pushFollow(FOLLOW_rule__ParameterKeyElement__Group__0_in_ruleParameterKeyElement515);
            rule__ParameterKeyElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getParameterKeyElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleParameterKeyElement"


    // $ANTLR start "entryRuleIssuesContainer"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:286:1: entryRuleIssuesContainer : ruleIssuesContainer EOF ;
    public final void entryRuleIssuesContainer() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:287:1: ( ruleIssuesContainer EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:288:1: ruleIssuesContainer EOF
            {
             before(grammarAccess.getIssuesContainerRule()); 
            pushFollow(FOLLOW_ruleIssuesContainer_in_entryRuleIssuesContainer542);
            ruleIssuesContainer();

            state._fsp--;

             after(grammarAccess.getIssuesContainerRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssuesContainer549); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssuesContainer"


    // $ANTLR start "ruleIssuesContainer"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:295:1: ruleIssuesContainer : ( ( rule__IssuesContainer__Group__0 ) ) ;
    public final void ruleIssuesContainer() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:299:2: ( ( ( rule__IssuesContainer__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:300:1: ( ( rule__IssuesContainer__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:300:1: ( ( rule__IssuesContainer__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:301:1: ( rule__IssuesContainer__Group__0 )
            {
             before(grammarAccess.getIssuesContainerAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:302:1: ( rule__IssuesContainer__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:302:2: rule__IssuesContainer__Group__0
            {
            pushFollow(FOLLOW_rule__IssuesContainer__Group__0_in_ruleIssuesContainer575);
            rule__IssuesContainer__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssuesContainerAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssuesContainer"


    // $ANTLR start "entryRuleIssuesTitleElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:314:1: entryRuleIssuesTitleElement : ruleIssuesTitleElement EOF ;
    public final void entryRuleIssuesTitleElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:315:1: ( ruleIssuesTitleElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:316:1: ruleIssuesTitleElement EOF
            {
             before(grammarAccess.getIssuesTitleElementRule()); 
            pushFollow(FOLLOW_ruleIssuesTitleElement_in_entryRuleIssuesTitleElement602);
            ruleIssuesTitleElement();

            state._fsp--;

             after(grammarAccess.getIssuesTitleElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssuesTitleElement609); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssuesTitleElement"


    // $ANTLR start "ruleIssuesTitleElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:323:1: ruleIssuesTitleElement : ( ( rule__IssuesTitleElement__NameAssignment ) ) ;
    public final void ruleIssuesTitleElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:327:2: ( ( ( rule__IssuesTitleElement__NameAssignment ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:328:1: ( ( rule__IssuesTitleElement__NameAssignment ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:328:1: ( ( rule__IssuesTitleElement__NameAssignment ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:329:1: ( rule__IssuesTitleElement__NameAssignment )
            {
             before(grammarAccess.getIssuesTitleElementAccess().getNameAssignment()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:330:1: ( rule__IssuesTitleElement__NameAssignment )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:330:2: rule__IssuesTitleElement__NameAssignment
            {
            pushFollow(FOLLOW_rule__IssuesTitleElement__NameAssignment_in_ruleIssuesTitleElement635);
            rule__IssuesTitleElement__NameAssignment();

            state._fsp--;


            }

             after(grammarAccess.getIssuesTitleElementAccess().getNameAssignment()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssuesTitleElement"


    // $ANTLR start "entryRuleIssueElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:342:1: entryRuleIssueElement : ruleIssueElement EOF ;
    public final void entryRuleIssueElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:343:1: ( ruleIssueElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:344:1: ruleIssueElement EOF
            {
             before(grammarAccess.getIssueElementRule()); 
            pushFollow(FOLLOW_ruleIssueElement_in_entryRuleIssueElement662);
            ruleIssueElement();

            state._fsp--;

             after(grammarAccess.getIssueElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueElement669); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueElement"


    // $ANTLR start "ruleIssueElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:351:1: ruleIssueElement : ( ( rule__IssueElement__Group__0 ) ) ;
    public final void ruleIssueElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:355:2: ( ( ( rule__IssueElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:356:1: ( ( rule__IssueElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:356:1: ( ( rule__IssueElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:357:1: ( rule__IssueElement__Group__0 )
            {
             before(grammarAccess.getIssueElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:358:1: ( rule__IssueElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:358:2: rule__IssueElement__Group__0
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__0_in_ruleIssueElement695);
            rule__IssueElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueElement"


    // $ANTLR start "entryRuleIssueSuppressComment"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:370:1: entryRuleIssueSuppressComment : ruleIssueSuppressComment EOF ;
    public final void entryRuleIssueSuppressComment() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:371:1: ( ruleIssueSuppressComment EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:372:1: ruleIssueSuppressComment EOF
            {
             before(grammarAccess.getIssueSuppressCommentRule()); 
            pushFollow(FOLLOW_ruleIssueSuppressComment_in_entryRuleIssueSuppressComment722);
            ruleIssueSuppressComment();

            state._fsp--;

             after(grammarAccess.getIssueSuppressCommentRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueSuppressComment729); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueSuppressComment"


    // $ANTLR start "ruleIssueSuppressComment"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:379:1: ruleIssueSuppressComment : ( ( rule__IssueSuppressComment__Group__0 ) ) ;
    public final void ruleIssueSuppressComment() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:383:2: ( ( ( rule__IssueSuppressComment__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:384:1: ( ( rule__IssueSuppressComment__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:384:1: ( ( rule__IssueSuppressComment__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:385:1: ( rule__IssueSuppressComment__Group__0 )
            {
             before(grammarAccess.getIssueSuppressCommentAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:386:1: ( rule__IssueSuppressComment__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:386:2: rule__IssueSuppressComment__Group__0
            {
            pushFollow(FOLLOW_rule__IssueSuppressComment__Group__0_in_ruleIssueSuppressComment755);
            rule__IssueSuppressComment__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueSuppressCommentAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueSuppressComment"


    // $ANTLR start "entryRuleIssueCategoryElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:398:1: entryRuleIssueCategoryElement : ruleIssueCategoryElement EOF ;
    public final void entryRuleIssueCategoryElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:399:1: ( ruleIssueCategoryElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:400:1: ruleIssueCategoryElement EOF
            {
             before(grammarAccess.getIssueCategoryElementRule()); 
            pushFollow(FOLLOW_ruleIssueCategoryElement_in_entryRuleIssueCategoryElement782);
            ruleIssueCategoryElement();

            state._fsp--;

             after(grammarAccess.getIssueCategoryElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueCategoryElement789); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueCategoryElement"


    // $ANTLR start "ruleIssueCategoryElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:407:1: ruleIssueCategoryElement : ( ( rule__IssueCategoryElement__Group__0 ) ) ;
    public final void ruleIssueCategoryElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:411:2: ( ( ( rule__IssueCategoryElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:412:1: ( ( rule__IssueCategoryElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:412:1: ( ( rule__IssueCategoryElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:413:1: ( rule__IssueCategoryElement__Group__0 )
            {
             before(grammarAccess.getIssueCategoryElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:414:1: ( rule__IssueCategoryElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:414:2: rule__IssueCategoryElement__Group__0
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__Group__0_in_ruleIssueCategoryElement815);
            rule__IssueCategoryElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueCategoryElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueCategoryElement"


    // $ANTLR start "entryRuleIssueKindElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:426:1: entryRuleIssueKindElement : ruleIssueKindElement EOF ;
    public final void entryRuleIssueKindElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:427:1: ( ruleIssueKindElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:428:1: ruleIssueKindElement EOF
            {
             before(grammarAccess.getIssueKindElementRule()); 
            pushFollow(FOLLOW_ruleIssueKindElement_in_entryRuleIssueKindElement842);
            ruleIssueKindElement();

            state._fsp--;

             after(grammarAccess.getIssueKindElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueKindElement849); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueKindElement"


    // $ANTLR start "ruleIssueKindElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:435:1: ruleIssueKindElement : ( ( rule__IssueKindElement__Group__0 ) ) ;
    public final void ruleIssueKindElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:439:2: ( ( ( rule__IssueKindElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:440:1: ( ( rule__IssueKindElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:440:1: ( ( rule__IssueKindElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:441:1: ( rule__IssueKindElement__Group__0 )
            {
             before(grammarAccess.getIssueKindElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:442:1: ( rule__IssueKindElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:442:2: rule__IssueKindElement__Group__0
            {
            pushFollow(FOLLOW_rule__IssueKindElement__Group__0_in_ruleIssueKindElement875);
            rule__IssueKindElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueKindElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueKindElement"


    // $ANTLR start "entryRuleIssueRelevanceElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:454:1: entryRuleIssueRelevanceElement : ruleIssueRelevanceElement EOF ;
    public final void entryRuleIssueRelevanceElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:455:1: ( ruleIssueRelevanceElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:456:1: ruleIssueRelevanceElement EOF
            {
             before(grammarAccess.getIssueRelevanceElementRule()); 
            pushFollow(FOLLOW_ruleIssueRelevanceElement_in_entryRuleIssueRelevanceElement902);
            ruleIssueRelevanceElement();

            state._fsp--;

             after(grammarAccess.getIssueRelevanceElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueRelevanceElement909); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueRelevanceElement"


    // $ANTLR start "ruleIssueRelevanceElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:463:1: ruleIssueRelevanceElement : ( ( rule__IssueRelevanceElement__Group__0 ) ) ;
    public final void ruleIssueRelevanceElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:467:2: ( ( ( rule__IssueRelevanceElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:468:1: ( ( rule__IssueRelevanceElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:468:1: ( ( rule__IssueRelevanceElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:469:1: ( rule__IssueRelevanceElement__Group__0 )
            {
             before(grammarAccess.getIssueRelevanceElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:470:1: ( rule__IssueRelevanceElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:470:2: rule__IssueRelevanceElement__Group__0
            {
            pushFollow(FOLLOW_rule__IssueRelevanceElement__Group__0_in_ruleIssueRelevanceElement935);
            rule__IssueRelevanceElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueRelevanceElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueRelevanceElement"


    // $ANTLR start "entryRuleIssuePackageElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:482:1: entryRuleIssuePackageElement : ruleIssuePackageElement EOF ;
    public final void entryRuleIssuePackageElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:483:1: ( ruleIssuePackageElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:484:1: ruleIssuePackageElement EOF
            {
             before(grammarAccess.getIssuePackageElementRule()); 
            pushFollow(FOLLOW_ruleIssuePackageElement_in_entryRuleIssuePackageElement962);
            ruleIssuePackageElement();

            state._fsp--;

             after(grammarAccess.getIssuePackageElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssuePackageElement969); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssuePackageElement"


    // $ANTLR start "ruleIssuePackageElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:491:1: ruleIssuePackageElement : ( ( rule__IssuePackageElement__Group__0 ) ) ;
    public final void ruleIssuePackageElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:495:2: ( ( ( rule__IssuePackageElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:496:1: ( ( rule__IssuePackageElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:496:1: ( ( rule__IssuePackageElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:497:1: ( rule__IssuePackageElement__Group__0 )
            {
             before(grammarAccess.getIssuePackageElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:498:1: ( rule__IssuePackageElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:498:2: rule__IssuePackageElement__Group__0
            {
            pushFollow(FOLLOW_rule__IssuePackageElement__Group__0_in_ruleIssuePackageElement995);
            rule__IssuePackageElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssuePackageElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssuePackageElement"


    // $ANTLR start "entryRuleIssueClassElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:510:1: entryRuleIssueClassElement : ruleIssueClassElement EOF ;
    public final void entryRuleIssueClassElement() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:511:1: ( ruleIssueClassElement EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:512:1: ruleIssueClassElement EOF
            {
             before(grammarAccess.getIssueClassElementRule()); 
            pushFollow(FOLLOW_ruleIssueClassElement_in_entryRuleIssueClassElement1022);
            ruleIssueClassElement();

            state._fsp--;

             after(grammarAccess.getIssueClassElementRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueClassElement1029); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueClassElement"


    // $ANTLR start "ruleIssueClassElement"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:519:1: ruleIssueClassElement : ( ( rule__IssueClassElement__Group__0 ) ) ;
    public final void ruleIssueClassElement() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:523:2: ( ( ( rule__IssueClassElement__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:524:1: ( ( rule__IssueClassElement__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:524:1: ( ( rule__IssueClassElement__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:525:1: ( rule__IssueClassElement__Group__0 )
            {
             before(grammarAccess.getIssueClassElementAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:526:1: ( rule__IssueClassElement__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:526:2: rule__IssueClassElement__Group__0
            {
            pushFollow(FOLLOW_rule__IssueClassElement__Group__0_in_ruleIssueClassElement1055);
            rule__IssueClassElement__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueClassElementAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueClassElement"


    // $ANTLR start "entryRuleIssueTypes"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:538:1: entryRuleIssueTypes : ruleIssueTypes EOF ;
    public final void entryRuleIssueTypes() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:539:1: ( ruleIssueTypes EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:540:1: ruleIssueTypes EOF
            {
             before(grammarAccess.getIssueTypesRule()); 
            pushFollow(FOLLOW_ruleIssueTypes_in_entryRuleIssueTypes1082);
            ruleIssueTypes();

            state._fsp--;

             after(grammarAccess.getIssueTypesRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueTypes1089); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueTypes"


    // $ANTLR start "ruleIssueTypes"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:547:1: ruleIssueTypes : ( ( rule__IssueTypes__Alternatives ) ) ;
    public final void ruleIssueTypes() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:551:2: ( ( ( rule__IssueTypes__Alternatives ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:552:1: ( ( rule__IssueTypes__Alternatives ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:552:1: ( ( rule__IssueTypes__Alternatives ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:553:1: ( rule__IssueTypes__Alternatives )
            {
             before(grammarAccess.getIssueTypesAccess().getAlternatives()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:554:1: ( rule__IssueTypes__Alternatives )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:554:2: rule__IssueTypes__Alternatives
            {
            pushFollow(FOLLOW_rule__IssueTypes__Alternatives_in_ruleIssueTypes1115);
            rule__IssueTypes__Alternatives();

            state._fsp--;


            }

             after(grammarAccess.getIssueTypesAccess().getAlternatives()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueTypes"


    // $ANTLR start "entryRuleIssueCategories"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:566:1: entryRuleIssueCategories : ruleIssueCategories EOF ;
    public final void entryRuleIssueCategories() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:567:1: ( ruleIssueCategories EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:568:1: ruleIssueCategories EOF
            {
             before(grammarAccess.getIssueCategoriesRule()); 
            pushFollow(FOLLOW_ruleIssueCategories_in_entryRuleIssueCategories1142);
            ruleIssueCategories();

            state._fsp--;

             after(grammarAccess.getIssueCategoriesRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueCategories1149); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueCategories"


    // $ANTLR start "ruleIssueCategories"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:575:1: ruleIssueCategories : ( ( rule__IssueCategories__Alternatives ) ) ;
    public final void ruleIssueCategories() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:579:2: ( ( ( rule__IssueCategories__Alternatives ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:580:1: ( ( rule__IssueCategories__Alternatives ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:580:1: ( ( rule__IssueCategories__Alternatives ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:581:1: ( rule__IssueCategories__Alternatives )
            {
             before(grammarAccess.getIssueCategoriesAccess().getAlternatives()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:582:1: ( rule__IssueCategories__Alternatives )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:582:2: rule__IssueCategories__Alternatives
            {
            pushFollow(FOLLOW_rule__IssueCategories__Alternatives_in_ruleIssueCategories1175);
            rule__IssueCategories__Alternatives();

            state._fsp--;


            }

             after(grammarAccess.getIssueCategoriesAccess().getAlternatives()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueCategories"


    // $ANTLR start "entryRuleIssueKinds"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:594:1: entryRuleIssueKinds : ruleIssueKinds EOF ;
    public final void entryRuleIssueKinds() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:595:1: ( ruleIssueKinds EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:596:1: ruleIssueKinds EOF
            {
             before(grammarAccess.getIssueKindsRule()); 
            pushFollow(FOLLOW_ruleIssueKinds_in_entryRuleIssueKinds1202);
            ruleIssueKinds();

            state._fsp--;

             after(grammarAccess.getIssueKindsRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueKinds1209); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueKinds"


    // $ANTLR start "ruleIssueKinds"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:603:1: ruleIssueKinds : ( ( rule__IssueKinds__Alternatives ) ) ;
    public final void ruleIssueKinds() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:607:2: ( ( ( rule__IssueKinds__Alternatives ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:608:1: ( ( rule__IssueKinds__Alternatives ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:608:1: ( ( rule__IssueKinds__Alternatives ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:609:1: ( rule__IssueKinds__Alternatives )
            {
             before(grammarAccess.getIssueKindsAccess().getAlternatives()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:610:1: ( rule__IssueKinds__Alternatives )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:610:2: rule__IssueKinds__Alternatives
            {
            pushFollow(FOLLOW_rule__IssueKinds__Alternatives_in_ruleIssueKinds1235);
            rule__IssueKinds__Alternatives();

            state._fsp--;


            }

             after(grammarAccess.getIssueKindsAccess().getAlternatives()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueKinds"


    // $ANTLR start "entryRuleIssueClass"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:622:1: entryRuleIssueClass : ruleIssueClass EOF ;
    public final void entryRuleIssueClass() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:623:1: ( ruleIssueClass EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:624:1: ruleIssueClass EOF
            {
             before(grammarAccess.getIssueClassRule()); 
            pushFollow(FOLLOW_ruleIssueClass_in_entryRuleIssueClass1262);
            ruleIssueClass();

            state._fsp--;

             after(grammarAccess.getIssueClassRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueClass1269); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleIssueClass"


    // $ANTLR start "ruleIssueClass"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:631:1: ruleIssueClass : ( ( rule__IssueClass__Group__0 ) ) ;
    public final void ruleIssueClass() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:635:2: ( ( ( rule__IssueClass__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:636:1: ( ( rule__IssueClass__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:636:1: ( ( rule__IssueClass__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:637:1: ( rule__IssueClass__Group__0 )
            {
             before(grammarAccess.getIssueClassAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:638:1: ( rule__IssueClass__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:638:2: rule__IssueClass__Group__0
            {
            pushFollow(FOLLOW_rule__IssueClass__Group__0_in_ruleIssueClass1295);
            rule__IssueClass__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getIssueClassAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleIssueClass"


    // $ANTLR start "entryRuleAnyValues"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:650:1: entryRuleAnyValues : ruleAnyValues EOF ;
    public final void entryRuleAnyValues() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:651:1: ( ruleAnyValues EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:652:1: ruleAnyValues EOF
            {
             before(grammarAccess.getAnyValuesRule()); 
            pushFollow(FOLLOW_ruleAnyValues_in_entryRuleAnyValues1322);
            ruleAnyValues();

            state._fsp--;

             after(grammarAccess.getAnyValuesRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleAnyValues1329); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleAnyValues"


    // $ANTLR start "ruleAnyValues"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:659:1: ruleAnyValues : ( ( rule__AnyValues__Alternatives ) ) ;
    public final void ruleAnyValues() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:663:2: ( ( ( rule__AnyValues__Alternatives ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:664:1: ( ( rule__AnyValues__Alternatives ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:664:1: ( ( rule__AnyValues__Alternatives ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:665:1: ( rule__AnyValues__Alternatives )
            {
             before(grammarAccess.getAnyValuesAccess().getAlternatives()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:666:1: ( rule__AnyValues__Alternatives )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:666:2: rule__AnyValues__Alternatives
            {
            pushFollow(FOLLOW_rule__AnyValues__Alternatives_in_ruleAnyValues1355);
            rule__AnyValues__Alternatives();

            state._fsp--;


            }

             after(grammarAccess.getAnyValuesAccess().getAlternatives()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleAnyValues"


    // $ANTLR start "entryRuleSlashPath"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:682:1: entryRuleSlashPath : ruleSlashPath EOF ;
    public final void entryRuleSlashPath() throws RecognitionException {
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:683:1: ( ruleSlashPath EOF )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:684:1: ruleSlashPath EOF
            {
             before(grammarAccess.getSlashPathRule()); 
            pushFollow(FOLLOW_ruleSlashPath_in_entryRuleSlashPath1386);
            ruleSlashPath();

            state._fsp--;

             after(grammarAccess.getSlashPathRule()); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleSlashPath1393); 

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return ;
    }
    // $ANTLR end "entryRuleSlashPath"


    // $ANTLR start "ruleSlashPath"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:691:1: ruleSlashPath : ( ( rule__SlashPath__Group__0 ) ) ;
    public final void ruleSlashPath() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:695:2: ( ( ( rule__SlashPath__Group__0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:696:1: ( ( rule__SlashPath__Group__0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:696:1: ( ( rule__SlashPath__Group__0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:697:1: ( rule__SlashPath__Group__0 )
            {
             before(grammarAccess.getSlashPathAccess().getGroup()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:698:1: ( rule__SlashPath__Group__0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:698:2: rule__SlashPath__Group__0
            {
            pushFollow(FOLLOW_rule__SlashPath__Group__0_in_ruleSlashPath1419);
            rule__SlashPath__Group__0();

            state._fsp--;


            }

             after(grammarAccess.getSlashPathAccess().getGroup()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "ruleSlashPath"


    // $ANTLR start "rule__AnalysisElement__Alternatives_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:712:1: rule__AnalysisElement__Alternatives_1 : ( ( RULE_ID ) | ( ':' ) | ( '/' ) | ( '\\\\' ) | ( ',' ) | ( '.' ) );
    public final void rule__AnalysisElement__Alternatives_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:716:1: ( ( RULE_ID ) | ( ':' ) | ( '/' ) | ( '\\\\' ) | ( ',' ) | ( '.' ) )
            int alt1=6;
            switch ( input.LA(1) ) {
            case RULE_ID:
                {
                alt1=1;
                }
                break;
            case 12:
                {
                alt1=2;
                }
                break;
            case 13:
                {
                alt1=3;
                }
                break;
            case 14:
                {
                alt1=4;
                }
                break;
            case 15:
                {
                alt1=5;
                }
                break;
            case 16:
                {
                alt1=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 1, 0, input);

                throw nvae;
            }

            switch (alt1) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:717:1: ( RULE_ID )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:717:1: ( RULE_ID )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:718:1: RULE_ID
                    {
                     before(grammarAccess.getAnalysisElementAccess().getIDTerminalRuleCall_1_0()); 
                    match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__AnalysisElement__Alternatives_11457); 
                     after(grammarAccess.getAnalysisElementAccess().getIDTerminalRuleCall_1_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:723:6: ( ':' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:723:6: ( ':' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:724:1: ':'
                    {
                     before(grammarAccess.getAnalysisElementAccess().getColonKeyword_1_1()); 
                    match(input,12,FOLLOW_12_in_rule__AnalysisElement__Alternatives_11475); 
                     after(grammarAccess.getAnalysisElementAccess().getColonKeyword_1_1()); 

                    }


                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:731:6: ( '/' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:731:6: ( '/' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:732:1: '/'
                    {
                     before(grammarAccess.getAnalysisElementAccess().getSolidusKeyword_1_2()); 
                    match(input,13,FOLLOW_13_in_rule__AnalysisElement__Alternatives_11495); 
                     after(grammarAccess.getAnalysisElementAccess().getSolidusKeyword_1_2()); 

                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:739:6: ( '\\\\' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:739:6: ( '\\\\' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:740:1: '\\\\'
                    {
                     before(grammarAccess.getAnalysisElementAccess().getReverseSolidusKeyword_1_3()); 
                    match(input,14,FOLLOW_14_in_rule__AnalysisElement__Alternatives_11515); 
                     after(grammarAccess.getAnalysisElementAccess().getReverseSolidusKeyword_1_3()); 

                    }


                    }
                    break;
                case 5 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:747:6: ( ',' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:747:6: ( ',' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:748:1: ','
                    {
                     before(grammarAccess.getAnalysisElementAccess().getCommaKeyword_1_4()); 
                    match(input,15,FOLLOW_15_in_rule__AnalysisElement__Alternatives_11535); 
                     after(grammarAccess.getAnalysisElementAccess().getCommaKeyword_1_4()); 

                    }


                    }
                    break;
                case 6 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:755:6: ( '.' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:755:6: ( '.' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:756:1: '.'
                    {
                     before(grammarAccess.getAnalysisElementAccess().getFullStopKeyword_1_5()); 
                    match(input,16,FOLLOW_16_in_rule__AnalysisElement__Alternatives_11555); 
                     after(grammarAccess.getAnalysisElementAccess().getFullStopKeyword_1_5()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnalysisElement__Alternatives_1"


    // $ANTLR start "rule__ParameterElement__Alternatives"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:768:1: rule__ParameterElement__Alternatives : ( ( ruleParameterKeyValueElement ) | ( ruleParameterKeyElement ) );
    public final void rule__ParameterElement__Alternatives() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:772:1: ( ( ruleParameterKeyValueElement ) | ( ruleParameterKeyElement ) )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==RULE_ID) ) {
                int LA2_1 = input.LA(2);

                if ( (LA2_1==12||LA2_1==17) ) {
                    alt2=1;
                }
                else if ( (LA2_1==30) ) {
                    alt2=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 2, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:773:1: ( ruleParameterKeyValueElement )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:773:1: ( ruleParameterKeyValueElement )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:774:1: ruleParameterKeyValueElement
                    {
                     before(grammarAccess.getParameterElementAccess().getParameterKeyValueElementParserRuleCall_0()); 
                    pushFollow(FOLLOW_ruleParameterKeyValueElement_in_rule__ParameterElement__Alternatives1589);
                    ruleParameterKeyValueElement();

                    state._fsp--;

                     after(grammarAccess.getParameterElementAccess().getParameterKeyValueElementParserRuleCall_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:779:6: ( ruleParameterKeyElement )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:779:6: ( ruleParameterKeyElement )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:780:1: ruleParameterKeyElement
                    {
                     before(grammarAccess.getParameterElementAccess().getParameterKeyElementParserRuleCall_1()); 
                    pushFollow(FOLLOW_ruleParameterKeyElement_in_rule__ParameterElement__Alternatives1606);
                    ruleParameterKeyElement();

                    state._fsp--;

                     after(grammarAccess.getParameterElementAccess().getParameterKeyElementParserRuleCall_1()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterElement__Alternatives"


    // $ANTLR start "rule__ParameterKeyValueElement__Alternatives_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:790:1: rule__ParameterKeyValueElement__Alternatives_1 : ( ( ':' ) | ( '=' ) );
    public final void rule__ParameterKeyValueElement__Alternatives_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:794:1: ( ( ':' ) | ( '=' ) )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==12) ) {
                alt3=1;
            }
            else if ( (LA3_0==17) ) {
                alt3=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:795:1: ( ':' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:795:1: ( ':' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:796:1: ':'
                    {
                     before(grammarAccess.getParameterKeyValueElementAccess().getColonKeyword_1_0()); 
                    match(input,12,FOLLOW_12_in_rule__ParameterKeyValueElement__Alternatives_11639); 
                     after(grammarAccess.getParameterKeyValueElementAccess().getColonKeyword_1_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:803:6: ( '=' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:803:6: ( '=' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:804:1: '='
                    {
                     before(grammarAccess.getParameterKeyValueElementAccess().getEqualsSignKeyword_1_1()); 
                    match(input,17,FOLLOW_17_in_rule__ParameterKeyValueElement__Alternatives_11659); 
                     after(grammarAccess.getParameterKeyValueElementAccess().getEqualsSignKeyword_1_1()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Alternatives_1"


    // $ANTLR start "rule__IssueSuppressComment__ValueAlternatives_1_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:816:1: rule__IssueSuppressComment__ValueAlternatives_1_0 : ( ( RULE_ID ) | ( RULE_STRING ) );
    public final void rule__IssueSuppressComment__ValueAlternatives_1_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:820:1: ( ( RULE_ID ) | ( RULE_STRING ) )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==RULE_ID) ) {
                alt4=1;
            }
            else if ( (LA4_0==RULE_STRING) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:821:1: ( RULE_ID )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:821:1: ( RULE_ID )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:822:1: RULE_ID
                    {
                     before(grammarAccess.getIssueSuppressCommentAccess().getValueIDTerminalRuleCall_1_0_0()); 
                    match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__IssueSuppressComment__ValueAlternatives_1_01693); 
                     after(grammarAccess.getIssueSuppressCommentAccess().getValueIDTerminalRuleCall_1_0_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:827:6: ( RULE_STRING )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:827:6: ( RULE_STRING )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:828:1: RULE_STRING
                    {
                     before(grammarAccess.getIssueSuppressCommentAccess().getValueSTRINGTerminalRuleCall_1_0_1()); 
                    match(input,RULE_STRING,FOLLOW_RULE_STRING_in_rule__IssueSuppressComment__ValueAlternatives_1_01710); 
                     after(grammarAccess.getIssueSuppressCommentAccess().getValueSTRINGTerminalRuleCall_1_0_1()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__ValueAlternatives_1_0"


    // $ANTLR start "rule__IssueTypes__Alternatives"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:838:1: rule__IssueTypes__Alternatives : ( ( 'DeadEdgesAnalysis' ) | ( 'UnusedLocalVariables' ) | ( 'GuardedAndUnguardedAccessAnalysis' ) | ( 'UnusedMethodsAnalysis' ) | ( 'UselessComputationsAnalysis' ) );
    public final void rule__IssueTypes__Alternatives() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:842:1: ( ( 'DeadEdgesAnalysis' ) | ( 'UnusedLocalVariables' ) | ( 'GuardedAndUnguardedAccessAnalysis' ) | ( 'UnusedMethodsAnalysis' ) | ( 'UselessComputationsAnalysis' ) )
            int alt5=5;
            switch ( input.LA(1) ) {
            case 18:
                {
                alt5=1;
                }
                break;
            case 19:
                {
                alt5=2;
                }
                break;
            case 20:
                {
                alt5=3;
                }
                break;
            case 21:
                {
                alt5=4;
                }
                break;
            case 22:
                {
                alt5=5;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }

            switch (alt5) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:843:1: ( 'DeadEdgesAnalysis' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:843:1: ( 'DeadEdgesAnalysis' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:844:1: 'DeadEdgesAnalysis'
                    {
                     before(grammarAccess.getIssueTypesAccess().getDeadEdgesAnalysisKeyword_0()); 
                    match(input,18,FOLLOW_18_in_rule__IssueTypes__Alternatives1743); 
                     after(grammarAccess.getIssueTypesAccess().getDeadEdgesAnalysisKeyword_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:851:6: ( 'UnusedLocalVariables' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:851:6: ( 'UnusedLocalVariables' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:852:1: 'UnusedLocalVariables'
                    {
                     before(grammarAccess.getIssueTypesAccess().getUnusedLocalVariablesKeyword_1()); 
                    match(input,19,FOLLOW_19_in_rule__IssueTypes__Alternatives1763); 
                     after(grammarAccess.getIssueTypesAccess().getUnusedLocalVariablesKeyword_1()); 

                    }


                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:859:6: ( 'GuardedAndUnguardedAccessAnalysis' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:859:6: ( 'GuardedAndUnguardedAccessAnalysis' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:860:1: 'GuardedAndUnguardedAccessAnalysis'
                    {
                     before(grammarAccess.getIssueTypesAccess().getGuardedAndUnguardedAccessAnalysisKeyword_2()); 
                    match(input,20,FOLLOW_20_in_rule__IssueTypes__Alternatives1783); 
                     after(grammarAccess.getIssueTypesAccess().getGuardedAndUnguardedAccessAnalysisKeyword_2()); 

                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:867:6: ( 'UnusedMethodsAnalysis' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:867:6: ( 'UnusedMethodsAnalysis' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:868:1: 'UnusedMethodsAnalysis'
                    {
                     before(grammarAccess.getIssueTypesAccess().getUnusedMethodsAnalysisKeyword_3()); 
                    match(input,21,FOLLOW_21_in_rule__IssueTypes__Alternatives1803); 
                     after(grammarAccess.getIssueTypesAccess().getUnusedMethodsAnalysisKeyword_3()); 

                    }


                    }
                    break;
                case 5 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:875:6: ( 'UselessComputationsAnalysis' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:875:6: ( 'UselessComputationsAnalysis' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:876:1: 'UselessComputationsAnalysis'
                    {
                     before(grammarAccess.getIssueTypesAccess().getUselessComputationsAnalysisKeyword_4()); 
                    match(input,22,FOLLOW_22_in_rule__IssueTypes__Alternatives1823); 
                     after(grammarAccess.getIssueTypesAccess().getUselessComputationsAnalysisKeyword_4()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueTypes__Alternatives"


    // $ANTLR start "rule__IssueCategories__Alternatives"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:888:1: rule__IssueCategories__Alternatives : ( ( ( rule__IssueCategories__BugAssignment_0 ) ) | ( ( rule__IssueCategories__SmellAssignment_1 ) ) | ( ( rule__IssueCategories__PerformanceAssignment_2 ) ) | ( ( rule__IssueCategories__ComprehensibilityAssignment_3 ) ) );
    public final void rule__IssueCategories__Alternatives() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:892:1: ( ( ( rule__IssueCategories__BugAssignment_0 ) ) | ( ( rule__IssueCategories__SmellAssignment_1 ) ) | ( ( rule__IssueCategories__PerformanceAssignment_2 ) ) | ( ( rule__IssueCategories__ComprehensibilityAssignment_3 ) ) )
            int alt6=4;
            switch ( input.LA(1) ) {
            case 42:
                {
                alt6=1;
                }
                break;
            case 43:
                {
                alt6=2;
                }
                break;
            case 44:
                {
                alt6=3;
                }
                break;
            case 45:
                {
                alt6=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 6, 0, input);

                throw nvae;
            }

            switch (alt6) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:893:1: ( ( rule__IssueCategories__BugAssignment_0 ) )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:893:1: ( ( rule__IssueCategories__BugAssignment_0 ) )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:894:1: ( rule__IssueCategories__BugAssignment_0 )
                    {
                     before(grammarAccess.getIssueCategoriesAccess().getBugAssignment_0()); 
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:895:1: ( rule__IssueCategories__BugAssignment_0 )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:895:2: rule__IssueCategories__BugAssignment_0
                    {
                    pushFollow(FOLLOW_rule__IssueCategories__BugAssignment_0_in_rule__IssueCategories__Alternatives1857);
                    rule__IssueCategories__BugAssignment_0();

                    state._fsp--;


                    }

                     after(grammarAccess.getIssueCategoriesAccess().getBugAssignment_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:899:6: ( ( rule__IssueCategories__SmellAssignment_1 ) )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:899:6: ( ( rule__IssueCategories__SmellAssignment_1 ) )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:900:1: ( rule__IssueCategories__SmellAssignment_1 )
                    {
                     before(grammarAccess.getIssueCategoriesAccess().getSmellAssignment_1()); 
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:901:1: ( rule__IssueCategories__SmellAssignment_1 )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:901:2: rule__IssueCategories__SmellAssignment_1
                    {
                    pushFollow(FOLLOW_rule__IssueCategories__SmellAssignment_1_in_rule__IssueCategories__Alternatives1875);
                    rule__IssueCategories__SmellAssignment_1();

                    state._fsp--;


                    }

                     after(grammarAccess.getIssueCategoriesAccess().getSmellAssignment_1()); 

                    }


                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:905:6: ( ( rule__IssueCategories__PerformanceAssignment_2 ) )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:905:6: ( ( rule__IssueCategories__PerformanceAssignment_2 ) )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:906:1: ( rule__IssueCategories__PerformanceAssignment_2 )
                    {
                     before(grammarAccess.getIssueCategoriesAccess().getPerformanceAssignment_2()); 
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:907:1: ( rule__IssueCategories__PerformanceAssignment_2 )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:907:2: rule__IssueCategories__PerformanceAssignment_2
                    {
                    pushFollow(FOLLOW_rule__IssueCategories__PerformanceAssignment_2_in_rule__IssueCategories__Alternatives1893);
                    rule__IssueCategories__PerformanceAssignment_2();

                    state._fsp--;


                    }

                     after(grammarAccess.getIssueCategoriesAccess().getPerformanceAssignment_2()); 

                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:911:6: ( ( rule__IssueCategories__ComprehensibilityAssignment_3 ) )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:911:6: ( ( rule__IssueCategories__ComprehensibilityAssignment_3 ) )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:912:1: ( rule__IssueCategories__ComprehensibilityAssignment_3 )
                    {
                     before(grammarAccess.getIssueCategoriesAccess().getComprehensibilityAssignment_3()); 
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:913:1: ( rule__IssueCategories__ComprehensibilityAssignment_3 )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:913:2: rule__IssueCategories__ComprehensibilityAssignment_3
                    {
                    pushFollow(FOLLOW_rule__IssueCategories__ComprehensibilityAssignment_3_in_rule__IssueCategories__Alternatives1911);
                    rule__IssueCategories__ComprehensibilityAssignment_3();

                    state._fsp--;


                    }

                     after(grammarAccess.getIssueCategoriesAccess().getComprehensibilityAssignment_3()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategories__Alternatives"


    // $ANTLR start "rule__IssueKinds__Alternatives"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:922:1: rule__IssueKinds__Alternatives : ( ( 'constant computation' ) | ( 'dead path' ) | ( 'throws exception' ) | ( 'unguarded use' ) | ( 'unused' ) | ( 'useless' ) );
    public final void rule__IssueKinds__Alternatives() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:926:1: ( ( 'constant computation' ) | ( 'dead path' ) | ( 'throws exception' ) | ( 'unguarded use' ) | ( 'unused' ) | ( 'useless' ) )
            int alt7=6;
            switch ( input.LA(1) ) {
            case 23:
                {
                alt7=1;
                }
                break;
            case 24:
                {
                alt7=2;
                }
                break;
            case 25:
                {
                alt7=3;
                }
                break;
            case 26:
                {
                alt7=4;
                }
                break;
            case 27:
                {
                alt7=5;
                }
                break;
            case 28:
                {
                alt7=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;
            }

            switch (alt7) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:927:1: ( 'constant computation' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:927:1: ( 'constant computation' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:928:1: 'constant computation'
                    {
                     before(grammarAccess.getIssueKindsAccess().getConstantComputationKeyword_0()); 
                    match(input,23,FOLLOW_23_in_rule__IssueKinds__Alternatives1945); 
                     after(grammarAccess.getIssueKindsAccess().getConstantComputationKeyword_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:935:6: ( 'dead path' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:935:6: ( 'dead path' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:936:1: 'dead path'
                    {
                     before(grammarAccess.getIssueKindsAccess().getDeadPathKeyword_1()); 
                    match(input,24,FOLLOW_24_in_rule__IssueKinds__Alternatives1965); 
                     after(grammarAccess.getIssueKindsAccess().getDeadPathKeyword_1()); 

                    }


                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:943:6: ( 'throws exception' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:943:6: ( 'throws exception' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:944:1: 'throws exception'
                    {
                     before(grammarAccess.getIssueKindsAccess().getThrowsExceptionKeyword_2()); 
                    match(input,25,FOLLOW_25_in_rule__IssueKinds__Alternatives1985); 
                     after(grammarAccess.getIssueKindsAccess().getThrowsExceptionKeyword_2()); 

                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:951:6: ( 'unguarded use' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:951:6: ( 'unguarded use' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:952:1: 'unguarded use'
                    {
                     before(grammarAccess.getIssueKindsAccess().getUnguardedUseKeyword_3()); 
                    match(input,26,FOLLOW_26_in_rule__IssueKinds__Alternatives2005); 
                     after(grammarAccess.getIssueKindsAccess().getUnguardedUseKeyword_3()); 

                    }


                    }
                    break;
                case 5 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:959:6: ( 'unused' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:959:6: ( 'unused' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:960:1: 'unused'
                    {
                     before(grammarAccess.getIssueKindsAccess().getUnusedKeyword_4()); 
                    match(input,27,FOLLOW_27_in_rule__IssueKinds__Alternatives2025); 
                     after(grammarAccess.getIssueKindsAccess().getUnusedKeyword_4()); 

                    }


                    }
                    break;
                case 6 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:967:6: ( 'useless' )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:967:6: ( 'useless' )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:968:1: 'useless'
                    {
                     before(grammarAccess.getIssueKindsAccess().getUselessKeyword_5()); 
                    match(input,28,FOLLOW_28_in_rule__IssueKinds__Alternatives2045); 
                     after(grammarAccess.getIssueKindsAccess().getUselessKeyword_5()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKinds__Alternatives"


    // $ANTLR start "rule__AnyValues__Alternatives"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:980:1: rule__AnyValues__Alternatives : ( ( RULE_INT ) | ( RULE_ID ) | ( ( rule__AnyValues__Group_2__0 ) ) | ( ( rule__AnyValues__Group_3__0 ) ) );
    public final void rule__AnyValues__Alternatives() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:984:1: ( ( RULE_INT ) | ( RULE_ID ) | ( ( rule__AnyValues__Group_2__0 ) ) | ( ( rule__AnyValues__Group_3__0 ) ) )
            int alt8=4;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==RULE_INT) ) {
                switch ( input.LA(2) ) {
                case EOF:
                case 36:
                    {
                    alt8=1;
                    }
                    break;
                case RULE_ID:
                    {
                    int LA8_4 = input.LA(3);

                    if ( (LA8_4==12||LA8_4==17||LA8_4==30) ) {
                        alt8=1;
                    }
                    else if ( (LA8_4==EOF||LA8_4==RULE_ID||LA8_4==36) ) {
                        alt8=3;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 8, 4, input);

                        throw nvae;
                    }
                    }
                    break;
                case 16:
                    {
                    alt8=4;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 8, 1, input);

                    throw nvae;
                }

            }
            else if ( (LA8_0==RULE_ID) ) {
                alt8=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 8, 0, input);

                throw nvae;
            }
            switch (alt8) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:985:1: ( RULE_INT )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:985:1: ( RULE_INT )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:986:1: RULE_INT
                    {
                     before(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_0()); 
                    match(input,RULE_INT,FOLLOW_RULE_INT_in_rule__AnyValues__Alternatives2079); 
                     after(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_0()); 

                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:991:6: ( RULE_ID )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:991:6: ( RULE_ID )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:992:1: RULE_ID
                    {
                     before(grammarAccess.getAnyValuesAccess().getIDTerminalRuleCall_1()); 
                    match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__AnyValues__Alternatives2096); 
                     after(grammarAccess.getAnyValuesAccess().getIDTerminalRuleCall_1()); 

                    }


                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:997:6: ( ( rule__AnyValues__Group_2__0 ) )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:997:6: ( ( rule__AnyValues__Group_2__0 ) )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:998:1: ( rule__AnyValues__Group_2__0 )
                    {
                     before(grammarAccess.getAnyValuesAccess().getGroup_2()); 
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:999:1: ( rule__AnyValues__Group_2__0 )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:999:2: rule__AnyValues__Group_2__0
                    {
                    pushFollow(FOLLOW_rule__AnyValues__Group_2__0_in_rule__AnyValues__Alternatives2113);
                    rule__AnyValues__Group_2__0();

                    state._fsp--;


                    }

                     after(grammarAccess.getAnyValuesAccess().getGroup_2()); 

                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1003:6: ( ( rule__AnyValues__Group_3__0 ) )
                    {
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1003:6: ( ( rule__AnyValues__Group_3__0 ) )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1004:1: ( rule__AnyValues__Group_3__0 )
                    {
                     before(grammarAccess.getAnyValuesAccess().getGroup_3()); 
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1005:1: ( rule__AnyValues__Group_3__0 )
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1005:2: rule__AnyValues__Group_3__0
                    {
                    pushFollow(FOLLOW_rule__AnyValues__Group_3__0_in_rule__AnyValues__Alternatives2131);
                    rule__AnyValues__Group_3__0();

                    state._fsp--;


                    }

                     after(grammarAccess.getAnyValuesAccess().getGroup_3()); 

                    }


                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Alternatives"


    // $ANTLR start "rule__ModelContainer__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1017:1: rule__ModelContainer__Group__0 : rule__ModelContainer__Group__0__Impl rule__ModelContainer__Group__1 ;
    public final void rule__ModelContainer__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1021:1: ( rule__ModelContainer__Group__0__Impl rule__ModelContainer__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1022:2: rule__ModelContainer__Group__0__Impl rule__ModelContainer__Group__1
            {
            pushFollow(FOLLOW_rule__ModelContainer__Group__0__Impl_in_rule__ModelContainer__Group__02163);
            rule__ModelContainer__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__ModelContainer__Group__1_in_rule__ModelContainer__Group__02166);
            rule__ModelContainer__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__Group__0"


    // $ANTLR start "rule__ModelContainer__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1029:1: rule__ModelContainer__Group__0__Impl : ( ( rule__ModelContainer__NameAssignment_0 ) ) ;
    public final void rule__ModelContainer__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1033:1: ( ( ( rule__ModelContainer__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1034:1: ( ( rule__ModelContainer__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1034:1: ( ( rule__ModelContainer__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1035:1: ( rule__ModelContainer__NameAssignment_0 )
            {
             before(grammarAccess.getModelContainerAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1036:1: ( rule__ModelContainer__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1036:2: rule__ModelContainer__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__ModelContainer__NameAssignment_0_in_rule__ModelContainer__Group__0__Impl2193);
            rule__ModelContainer__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getModelContainerAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__Group__0__Impl"


    // $ANTLR start "rule__ModelContainer__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1046:1: rule__ModelContainer__Group__1 : rule__ModelContainer__Group__1__Impl rule__ModelContainer__Group__2 ;
    public final void rule__ModelContainer__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1050:1: ( rule__ModelContainer__Group__1__Impl rule__ModelContainer__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1051:2: rule__ModelContainer__Group__1__Impl rule__ModelContainer__Group__2
            {
            pushFollow(FOLLOW_rule__ModelContainer__Group__1__Impl_in_rule__ModelContainer__Group__12223);
            rule__ModelContainer__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__ModelContainer__Group__2_in_rule__ModelContainer__Group__12226);
            rule__ModelContainer__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__Group__1"


    // $ANTLR start "rule__ModelContainer__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1058:1: rule__ModelContainer__Group__1__Impl : ( ( rule__ModelContainer__ParameterAssignment_1 ) ) ;
    public final void rule__ModelContainer__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1062:1: ( ( ( rule__ModelContainer__ParameterAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1063:1: ( ( rule__ModelContainer__ParameterAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1063:1: ( ( rule__ModelContainer__ParameterAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1064:1: ( rule__ModelContainer__ParameterAssignment_1 )
            {
             before(grammarAccess.getModelContainerAccess().getParameterAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1065:1: ( rule__ModelContainer__ParameterAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1065:2: rule__ModelContainer__ParameterAssignment_1
            {
            pushFollow(FOLLOW_rule__ModelContainer__ParameterAssignment_1_in_rule__ModelContainer__Group__1__Impl2253);
            rule__ModelContainer__ParameterAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getModelContainerAccess().getParameterAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__Group__1__Impl"


    // $ANTLR start "rule__ModelContainer__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1075:1: rule__ModelContainer__Group__2 : rule__ModelContainer__Group__2__Impl ;
    public final void rule__ModelContainer__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1079:1: ( rule__ModelContainer__Group__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1080:2: rule__ModelContainer__Group__2__Impl
            {
            pushFollow(FOLLOW_rule__ModelContainer__Group__2__Impl_in_rule__ModelContainer__Group__22283);
            rule__ModelContainer__Group__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__Group__2"


    // $ANTLR start "rule__ModelContainer__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1086:1: rule__ModelContainer__Group__2__Impl : ( ( rule__ModelContainer__IssuesAssignment_2 ) ) ;
    public final void rule__ModelContainer__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1090:1: ( ( ( rule__ModelContainer__IssuesAssignment_2 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1091:1: ( ( rule__ModelContainer__IssuesAssignment_2 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1091:1: ( ( rule__ModelContainer__IssuesAssignment_2 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1092:1: ( rule__ModelContainer__IssuesAssignment_2 )
            {
             before(grammarAccess.getModelContainerAccess().getIssuesAssignment_2()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1093:1: ( rule__ModelContainer__IssuesAssignment_2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1093:2: rule__ModelContainer__IssuesAssignment_2
            {
            pushFollow(FOLLOW_rule__ModelContainer__IssuesAssignment_2_in_rule__ModelContainer__Group__2__Impl2310);
            rule__ModelContainer__IssuesAssignment_2();

            state._fsp--;


            }

             after(grammarAccess.getModelContainerAccess().getIssuesAssignment_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__Group__2__Impl"


    // $ANTLR start "rule__AnalysisElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1109:1: rule__AnalysisElement__Group__0 : rule__AnalysisElement__Group__0__Impl rule__AnalysisElement__Group__1 ;
    public final void rule__AnalysisElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1113:1: ( rule__AnalysisElement__Group__0__Impl rule__AnalysisElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1114:2: rule__AnalysisElement__Group__0__Impl rule__AnalysisElement__Group__1
            {
            pushFollow(FOLLOW_rule__AnalysisElement__Group__0__Impl_in_rule__AnalysisElement__Group__02346);
            rule__AnalysisElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__AnalysisElement__Group__1_in_rule__AnalysisElement__Group__02349);
            rule__AnalysisElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnalysisElement__Group__0"


    // $ANTLR start "rule__AnalysisElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1121:1: rule__AnalysisElement__Group__0__Impl : ( 'Analysis of ' ) ;
    public final void rule__AnalysisElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1125:1: ( ( 'Analysis of ' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1126:1: ( 'Analysis of ' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1126:1: ( 'Analysis of ' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1127:1: 'Analysis of '
            {
             before(grammarAccess.getAnalysisElementAccess().getAnalysisOfKeyword_0()); 
            match(input,29,FOLLOW_29_in_rule__AnalysisElement__Group__0__Impl2377); 
             after(grammarAccess.getAnalysisElementAccess().getAnalysisOfKeyword_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnalysisElement__Group__0__Impl"


    // $ANTLR start "rule__AnalysisElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1140:1: rule__AnalysisElement__Group__1 : rule__AnalysisElement__Group__1__Impl ;
    public final void rule__AnalysisElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1144:1: ( rule__AnalysisElement__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1145:2: rule__AnalysisElement__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__AnalysisElement__Group__1__Impl_in_rule__AnalysisElement__Group__12408);
            rule__AnalysisElement__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnalysisElement__Group__1"


    // $ANTLR start "rule__AnalysisElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1151:1: rule__AnalysisElement__Group__1__Impl : ( ( ( rule__AnalysisElement__Alternatives_1 ) ) ( ( rule__AnalysisElement__Alternatives_1 )* ) ) ;
    public final void rule__AnalysisElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1155:1: ( ( ( ( rule__AnalysisElement__Alternatives_1 ) ) ( ( rule__AnalysisElement__Alternatives_1 )* ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1156:1: ( ( ( rule__AnalysisElement__Alternatives_1 ) ) ( ( rule__AnalysisElement__Alternatives_1 )* ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1156:1: ( ( ( rule__AnalysisElement__Alternatives_1 ) ) ( ( rule__AnalysisElement__Alternatives_1 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1157:1: ( ( rule__AnalysisElement__Alternatives_1 ) ) ( ( rule__AnalysisElement__Alternatives_1 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1157:1: ( ( rule__AnalysisElement__Alternatives_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1158:1: ( rule__AnalysisElement__Alternatives_1 )
            {
             before(grammarAccess.getAnalysisElementAccess().getAlternatives_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1159:1: ( rule__AnalysisElement__Alternatives_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1159:2: rule__AnalysisElement__Alternatives_1
            {
            pushFollow(FOLLOW_rule__AnalysisElement__Alternatives_1_in_rule__AnalysisElement__Group__1__Impl2437);
            rule__AnalysisElement__Alternatives_1();

            state._fsp--;


            }

             after(grammarAccess.getAnalysisElementAccess().getAlternatives_1()); 

            }

            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1162:1: ( ( rule__AnalysisElement__Alternatives_1 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1163:1: ( rule__AnalysisElement__Alternatives_1 )*
            {
             before(grammarAccess.getAnalysisElementAccess().getAlternatives_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1164:1: ( rule__AnalysisElement__Alternatives_1 )*
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( (LA9_0==RULE_ID||(LA9_0>=12 && LA9_0<=16)) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1164:2: rule__AnalysisElement__Alternatives_1
            	    {
            	    pushFollow(FOLLOW_rule__AnalysisElement__Alternatives_1_in_rule__AnalysisElement__Group__1__Impl2449);
            	    rule__AnalysisElement__Alternatives_1();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop9;
                }
            } while (true);

             after(grammarAccess.getAnalysisElementAccess().getAlternatives_1()); 

            }


            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnalysisElement__Group__1__Impl"


    // $ANTLR start "rule__ParameterContainer__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1179:1: rule__ParameterContainer__Group__0 : rule__ParameterContainer__Group__0__Impl rule__ParameterContainer__Group__1 ;
    public final void rule__ParameterContainer__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1183:1: ( rule__ParameterContainer__Group__0__Impl rule__ParameterContainer__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1184:2: rule__ParameterContainer__Group__0__Impl rule__ParameterContainer__Group__1
            {
            pushFollow(FOLLOW_rule__ParameterContainer__Group__0__Impl_in_rule__ParameterContainer__Group__02486);
            rule__ParameterContainer__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__ParameterContainer__Group__1_in_rule__ParameterContainer__Group__02489);
            rule__ParameterContainer__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterContainer__Group__0"


    // $ANTLR start "rule__ParameterContainer__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1191:1: rule__ParameterContainer__Group__0__Impl : ( ( rule__ParameterContainer__NameAssignment_0 ) ) ;
    public final void rule__ParameterContainer__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1195:1: ( ( ( rule__ParameterContainer__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1196:1: ( ( rule__ParameterContainer__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1196:1: ( ( rule__ParameterContainer__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1197:1: ( rule__ParameterContainer__NameAssignment_0 )
            {
             before(grammarAccess.getParameterContainerAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1198:1: ( rule__ParameterContainer__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1198:2: rule__ParameterContainer__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__ParameterContainer__NameAssignment_0_in_rule__ParameterContainer__Group__0__Impl2516);
            rule__ParameterContainer__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getParameterContainerAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterContainer__Group__0__Impl"


    // $ANTLR start "rule__ParameterContainer__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1208:1: rule__ParameterContainer__Group__1 : rule__ParameterContainer__Group__1__Impl ;
    public final void rule__ParameterContainer__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1212:1: ( rule__ParameterContainer__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1213:2: rule__ParameterContainer__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__ParameterContainer__Group__1__Impl_in_rule__ParameterContainer__Group__12546);
            rule__ParameterContainer__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterContainer__Group__1"


    // $ANTLR start "rule__ParameterContainer__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1219:1: rule__ParameterContainer__Group__1__Impl : ( ( ( rule__ParameterContainer__ElementsAssignment_1 ) ) ( ( rule__ParameterContainer__ElementsAssignment_1 )* ) ) ;
    public final void rule__ParameterContainer__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1223:1: ( ( ( ( rule__ParameterContainer__ElementsAssignment_1 ) ) ( ( rule__ParameterContainer__ElementsAssignment_1 )* ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1224:1: ( ( ( rule__ParameterContainer__ElementsAssignment_1 ) ) ( ( rule__ParameterContainer__ElementsAssignment_1 )* ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1224:1: ( ( ( rule__ParameterContainer__ElementsAssignment_1 ) ) ( ( rule__ParameterContainer__ElementsAssignment_1 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1225:1: ( ( rule__ParameterContainer__ElementsAssignment_1 ) ) ( ( rule__ParameterContainer__ElementsAssignment_1 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1225:1: ( ( rule__ParameterContainer__ElementsAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1226:1: ( rule__ParameterContainer__ElementsAssignment_1 )
            {
             before(grammarAccess.getParameterContainerAccess().getElementsAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1227:1: ( rule__ParameterContainer__ElementsAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1227:2: rule__ParameterContainer__ElementsAssignment_1
            {
            pushFollow(FOLLOW_rule__ParameterContainer__ElementsAssignment_1_in_rule__ParameterContainer__Group__1__Impl2575);
            rule__ParameterContainer__ElementsAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getParameterContainerAccess().getElementsAssignment_1()); 

            }

            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1230:1: ( ( rule__ParameterContainer__ElementsAssignment_1 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1231:1: ( rule__ParameterContainer__ElementsAssignment_1 )*
            {
             before(grammarAccess.getParameterContainerAccess().getElementsAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1232:1: ( rule__ParameterContainer__ElementsAssignment_1 )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==RULE_ID) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1232:2: rule__ParameterContainer__ElementsAssignment_1
            	    {
            	    pushFollow(FOLLOW_rule__ParameterContainer__ElementsAssignment_1_in_rule__ParameterContainer__Group__1__Impl2587);
            	    rule__ParameterContainer__ElementsAssignment_1();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);

             after(grammarAccess.getParameterContainerAccess().getElementsAssignment_1()); 

            }


            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterContainer__Group__1__Impl"


    // $ANTLR start "rule__ParameterKeyValueElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1247:1: rule__ParameterKeyValueElement__Group__0 : rule__ParameterKeyValueElement__Group__0__Impl rule__ParameterKeyValueElement__Group__1 ;
    public final void rule__ParameterKeyValueElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1251:1: ( rule__ParameterKeyValueElement__Group__0__Impl rule__ParameterKeyValueElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1252:2: rule__ParameterKeyValueElement__Group__0__Impl rule__ParameterKeyValueElement__Group__1
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Group__0__Impl_in_rule__ParameterKeyValueElement__Group__02624);
            rule__ParameterKeyValueElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Group__1_in_rule__ParameterKeyValueElement__Group__02627);
            rule__ParameterKeyValueElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Group__0"


    // $ANTLR start "rule__ParameterKeyValueElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1259:1: rule__ParameterKeyValueElement__Group__0__Impl : ( ( rule__ParameterKeyValueElement__NameAssignment_0 ) ) ;
    public final void rule__ParameterKeyValueElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1263:1: ( ( ( rule__ParameterKeyValueElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1264:1: ( ( rule__ParameterKeyValueElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1264:1: ( ( rule__ParameterKeyValueElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1265:1: ( rule__ParameterKeyValueElement__NameAssignment_0 )
            {
             before(grammarAccess.getParameterKeyValueElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1266:1: ( rule__ParameterKeyValueElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1266:2: rule__ParameterKeyValueElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__NameAssignment_0_in_rule__ParameterKeyValueElement__Group__0__Impl2654);
            rule__ParameterKeyValueElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getParameterKeyValueElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Group__0__Impl"


    // $ANTLR start "rule__ParameterKeyValueElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1276:1: rule__ParameterKeyValueElement__Group__1 : rule__ParameterKeyValueElement__Group__1__Impl rule__ParameterKeyValueElement__Group__2 ;
    public final void rule__ParameterKeyValueElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1280:1: ( rule__ParameterKeyValueElement__Group__1__Impl rule__ParameterKeyValueElement__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1281:2: rule__ParameterKeyValueElement__Group__1__Impl rule__ParameterKeyValueElement__Group__2
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Group__1__Impl_in_rule__ParameterKeyValueElement__Group__12684);
            rule__ParameterKeyValueElement__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Group__2_in_rule__ParameterKeyValueElement__Group__12687);
            rule__ParameterKeyValueElement__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Group__1"


    // $ANTLR start "rule__ParameterKeyValueElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1288:1: rule__ParameterKeyValueElement__Group__1__Impl : ( ( rule__ParameterKeyValueElement__Alternatives_1 ) ) ;
    public final void rule__ParameterKeyValueElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1292:1: ( ( ( rule__ParameterKeyValueElement__Alternatives_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1293:1: ( ( rule__ParameterKeyValueElement__Alternatives_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1293:1: ( ( rule__ParameterKeyValueElement__Alternatives_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1294:1: ( rule__ParameterKeyValueElement__Alternatives_1 )
            {
             before(grammarAccess.getParameterKeyValueElementAccess().getAlternatives_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1295:1: ( rule__ParameterKeyValueElement__Alternatives_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1295:2: rule__ParameterKeyValueElement__Alternatives_1
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Alternatives_1_in_rule__ParameterKeyValueElement__Group__1__Impl2714);
            rule__ParameterKeyValueElement__Alternatives_1();

            state._fsp--;


            }

             after(grammarAccess.getParameterKeyValueElementAccess().getAlternatives_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Group__1__Impl"


    // $ANTLR start "rule__ParameterKeyValueElement__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1305:1: rule__ParameterKeyValueElement__Group__2 : rule__ParameterKeyValueElement__Group__2__Impl ;
    public final void rule__ParameterKeyValueElement__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1309:1: ( rule__ParameterKeyValueElement__Group__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1310:2: rule__ParameterKeyValueElement__Group__2__Impl
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__Group__2__Impl_in_rule__ParameterKeyValueElement__Group__22744);
            rule__ParameterKeyValueElement__Group__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Group__2"


    // $ANTLR start "rule__ParameterKeyValueElement__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1316:1: rule__ParameterKeyValueElement__Group__2__Impl : ( ( rule__ParameterKeyValueElement__ValueAssignment_2 ) ) ;
    public final void rule__ParameterKeyValueElement__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1320:1: ( ( ( rule__ParameterKeyValueElement__ValueAssignment_2 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1321:1: ( ( rule__ParameterKeyValueElement__ValueAssignment_2 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1321:1: ( ( rule__ParameterKeyValueElement__ValueAssignment_2 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1322:1: ( rule__ParameterKeyValueElement__ValueAssignment_2 )
            {
             before(grammarAccess.getParameterKeyValueElementAccess().getValueAssignment_2()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1323:1: ( rule__ParameterKeyValueElement__ValueAssignment_2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1323:2: rule__ParameterKeyValueElement__ValueAssignment_2
            {
            pushFollow(FOLLOW_rule__ParameterKeyValueElement__ValueAssignment_2_in_rule__ParameterKeyValueElement__Group__2__Impl2771);
            rule__ParameterKeyValueElement__ValueAssignment_2();

            state._fsp--;


            }

             after(grammarAccess.getParameterKeyValueElementAccess().getValueAssignment_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__Group__2__Impl"


    // $ANTLR start "rule__ParameterKeyElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1339:1: rule__ParameterKeyElement__Group__0 : rule__ParameterKeyElement__Group__0__Impl rule__ParameterKeyElement__Group__1 ;
    public final void rule__ParameterKeyElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1343:1: ( rule__ParameterKeyElement__Group__0__Impl rule__ParameterKeyElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1344:2: rule__ParameterKeyElement__Group__0__Impl rule__ParameterKeyElement__Group__1
            {
            pushFollow(FOLLOW_rule__ParameterKeyElement__Group__0__Impl_in_rule__ParameterKeyElement__Group__02807);
            rule__ParameterKeyElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__ParameterKeyElement__Group__1_in_rule__ParameterKeyElement__Group__02810);
            rule__ParameterKeyElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyElement__Group__0"


    // $ANTLR start "rule__ParameterKeyElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1351:1: rule__ParameterKeyElement__Group__0__Impl : ( ( rule__ParameterKeyElement__NameAssignment_0 ) ) ;
    public final void rule__ParameterKeyElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1355:1: ( ( ( rule__ParameterKeyElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1356:1: ( ( rule__ParameterKeyElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1356:1: ( ( rule__ParameterKeyElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1357:1: ( rule__ParameterKeyElement__NameAssignment_0 )
            {
             before(grammarAccess.getParameterKeyElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1358:1: ( rule__ParameterKeyElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1358:2: rule__ParameterKeyElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__ParameterKeyElement__NameAssignment_0_in_rule__ParameterKeyElement__Group__0__Impl2837);
            rule__ParameterKeyElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getParameterKeyElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyElement__Group__0__Impl"


    // $ANTLR start "rule__ParameterKeyElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1368:1: rule__ParameterKeyElement__Group__1 : rule__ParameterKeyElement__Group__1__Impl ;
    public final void rule__ParameterKeyElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1372:1: ( rule__ParameterKeyElement__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1373:2: rule__ParameterKeyElement__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__ParameterKeyElement__Group__1__Impl_in_rule__ParameterKeyElement__Group__12867);
            rule__ParameterKeyElement__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyElement__Group__1"


    // $ANTLR start "rule__ParameterKeyElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1379:1: rule__ParameterKeyElement__Group__1__Impl : ( ';' ) ;
    public final void rule__ParameterKeyElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1383:1: ( ( ';' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1384:1: ( ';' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1384:1: ( ';' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1385:1: ';'
            {
             before(grammarAccess.getParameterKeyElementAccess().getSemicolonKeyword_1()); 
            match(input,30,FOLLOW_30_in_rule__ParameterKeyElement__Group__1__Impl2895); 
             after(grammarAccess.getParameterKeyElementAccess().getSemicolonKeyword_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyElement__Group__1__Impl"


    // $ANTLR start "rule__IssuesContainer__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1402:1: rule__IssuesContainer__Group__0 : rule__IssuesContainer__Group__0__Impl rule__IssuesContainer__Group__1 ;
    public final void rule__IssuesContainer__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1406:1: ( rule__IssuesContainer__Group__0__Impl rule__IssuesContainer__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1407:2: rule__IssuesContainer__Group__0__Impl rule__IssuesContainer__Group__1
            {
            pushFollow(FOLLOW_rule__IssuesContainer__Group__0__Impl_in_rule__IssuesContainer__Group__02930);
            rule__IssuesContainer__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssuesContainer__Group__1_in_rule__IssuesContainer__Group__02933);
            rule__IssuesContainer__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuesContainer__Group__0"


    // $ANTLR start "rule__IssuesContainer__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1414:1: rule__IssuesContainer__Group__0__Impl : ( ruleIssuesTitleElement ) ;
    public final void rule__IssuesContainer__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1418:1: ( ( ruleIssuesTitleElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1419:1: ( ruleIssuesTitleElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1419:1: ( ruleIssuesTitleElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1420:1: ruleIssuesTitleElement
            {
             before(grammarAccess.getIssuesContainerAccess().getIssuesTitleElementParserRuleCall_0()); 
            pushFollow(FOLLOW_ruleIssuesTitleElement_in_rule__IssuesContainer__Group__0__Impl2960);
            ruleIssuesTitleElement();

            state._fsp--;

             after(grammarAccess.getIssuesContainerAccess().getIssuesTitleElementParserRuleCall_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuesContainer__Group__0__Impl"


    // $ANTLR start "rule__IssuesContainer__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1431:1: rule__IssuesContainer__Group__1 : rule__IssuesContainer__Group__1__Impl ;
    public final void rule__IssuesContainer__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1435:1: ( rule__IssuesContainer__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1436:2: rule__IssuesContainer__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__IssuesContainer__Group__1__Impl_in_rule__IssuesContainer__Group__12989);
            rule__IssuesContainer__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuesContainer__Group__1"


    // $ANTLR start "rule__IssuesContainer__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1442:1: rule__IssuesContainer__Group__1__Impl : ( ( rule__IssuesContainer__ElementsAssignment_1 )* ) ;
    public final void rule__IssuesContainer__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1446:1: ( ( ( rule__IssuesContainer__ElementsAssignment_1 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1447:1: ( ( rule__IssuesContainer__ElementsAssignment_1 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1447:1: ( ( rule__IssuesContainer__ElementsAssignment_1 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1448:1: ( rule__IssuesContainer__ElementsAssignment_1 )*
            {
             before(grammarAccess.getIssuesContainerAccess().getElementsAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1449:1: ( rule__IssuesContainer__ElementsAssignment_1 )*
            loop11:
            do {
                int alt11=2;
                int LA11_0 = input.LA(1);

                if ( ((LA11_0>=18 && LA11_0<=22)) ) {
                    alt11=1;
                }


                switch (alt11) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1449:2: rule__IssuesContainer__ElementsAssignment_1
            	    {
            	    pushFollow(FOLLOW_rule__IssuesContainer__ElementsAssignment_1_in_rule__IssuesContainer__Group__1__Impl3016);
            	    rule__IssuesContainer__ElementsAssignment_1();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop11;
                }
            } while (true);

             after(grammarAccess.getIssuesContainerAccess().getElementsAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuesContainer__Group__1__Impl"


    // $ANTLR start "rule__IssueElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1463:1: rule__IssueElement__Group__0 : rule__IssueElement__Group__0__Impl rule__IssueElement__Group__1 ;
    public final void rule__IssueElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1467:1: ( rule__IssueElement__Group__0__Impl rule__IssueElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1468:2: rule__IssueElement__Group__0__Impl rule__IssueElement__Group__1
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__0__Impl_in_rule__IssueElement__Group__03051);
            rule__IssueElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__1_in_rule__IssueElement__Group__03054);
            rule__IssueElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__0"


    // $ANTLR start "rule__IssueElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1475:1: rule__IssueElement__Group__0__Impl : ( ( rule__IssueElement__NameAssignment_0 ) ) ;
    public final void rule__IssueElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1479:1: ( ( ( rule__IssueElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1480:1: ( ( rule__IssueElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1480:1: ( ( rule__IssueElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1481:1: ( rule__IssueElement__NameAssignment_0 )
            {
             before(grammarAccess.getIssueElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1482:1: ( rule__IssueElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1482:2: rule__IssueElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__IssueElement__NameAssignment_0_in_rule__IssueElement__Group__0__Impl3081);
            rule__IssueElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__0__Impl"


    // $ANTLR start "rule__IssueElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1492:1: rule__IssueElement__Group__1 : rule__IssueElement__Group__1__Impl rule__IssueElement__Group__2 ;
    public final void rule__IssueElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1496:1: ( rule__IssueElement__Group__1__Impl rule__IssueElement__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1497:2: rule__IssueElement__Group__1__Impl rule__IssueElement__Group__2
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__1__Impl_in_rule__IssueElement__Group__13111);
            rule__IssueElement__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__2_in_rule__IssueElement__Group__13114);
            rule__IssueElement__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__1"


    // $ANTLR start "rule__IssueElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1504:1: rule__IssueElement__Group__1__Impl : ( ( rule__IssueElement__Group_1__0 )* ) ;
    public final void rule__IssueElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1508:1: ( ( ( rule__IssueElement__Group_1__0 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1509:1: ( ( rule__IssueElement__Group_1__0 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1509:1: ( ( rule__IssueElement__Group_1__0 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1510:1: ( rule__IssueElement__Group_1__0 )*
            {
             before(grammarAccess.getIssueElementAccess().getGroup_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1511:1: ( rule__IssueElement__Group_1__0 )*
            loop12:
            do {
                int alt12=2;
                int LA12_0 = input.LA(1);

                if ( (LA12_0==15) ) {
                    alt12=1;
                }


                switch (alt12) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1511:2: rule__IssueElement__Group_1__0
            	    {
            	    pushFollow(FOLLOW_rule__IssueElement__Group_1__0_in_rule__IssueElement__Group__1__Impl3141);
            	    rule__IssueElement__Group_1__0();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop12;
                }
            } while (true);

             after(grammarAccess.getIssueElementAccess().getGroup_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__1__Impl"


    // $ANTLR start "rule__IssueElement__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1521:1: rule__IssueElement__Group__2 : rule__IssueElement__Group__2__Impl rule__IssueElement__Group__3 ;
    public final void rule__IssueElement__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1525:1: ( rule__IssueElement__Group__2__Impl rule__IssueElement__Group__3 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1526:2: rule__IssueElement__Group__2__Impl rule__IssueElement__Group__3
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__2__Impl_in_rule__IssueElement__Group__23172);
            rule__IssueElement__Group__2__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__3_in_rule__IssueElement__Group__23175);
            rule__IssueElement__Group__3();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__2"


    // $ANTLR start "rule__IssueElement__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1533:1: rule__IssueElement__Group__2__Impl : ( ( rule__IssueElement__CommentAssignment_2 )? ) ;
    public final void rule__IssueElement__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1537:1: ( ( ( rule__IssueElement__CommentAssignment_2 )? ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1538:1: ( ( rule__IssueElement__CommentAssignment_2 )? )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1538:1: ( ( rule__IssueElement__CommentAssignment_2 )? )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1539:1: ( rule__IssueElement__CommentAssignment_2 )?
            {
             before(grammarAccess.getIssueElementAccess().getCommentAssignment_2()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1540:1: ( rule__IssueElement__CommentAssignment_2 )?
            int alt13=2;
            int LA13_0 = input.LA(1);

            if ( (LA13_0==33) ) {
                alt13=1;
            }
            switch (alt13) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1540:2: rule__IssueElement__CommentAssignment_2
                    {
                    pushFollow(FOLLOW_rule__IssueElement__CommentAssignment_2_in_rule__IssueElement__Group__2__Impl3202);
                    rule__IssueElement__CommentAssignment_2();

                    state._fsp--;


                    }
                    break;

            }

             after(grammarAccess.getIssueElementAccess().getCommentAssignment_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__2__Impl"


    // $ANTLR start "rule__IssueElement__Group__3"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1550:1: rule__IssueElement__Group__3 : rule__IssueElement__Group__3__Impl rule__IssueElement__Group__4 ;
    public final void rule__IssueElement__Group__3() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1554:1: ( rule__IssueElement__Group__3__Impl rule__IssueElement__Group__4 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1555:2: rule__IssueElement__Group__3__Impl rule__IssueElement__Group__4
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__3__Impl_in_rule__IssueElement__Group__33233);
            rule__IssueElement__Group__3__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__4_in_rule__IssueElement__Group__33236);
            rule__IssueElement__Group__4();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__3"


    // $ANTLR start "rule__IssueElement__Group__3__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1562:1: rule__IssueElement__Group__3__Impl : ( '{' ) ;
    public final void rule__IssueElement__Group__3__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1566:1: ( ( '{' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1567:1: ( '{' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1567:1: ( '{' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1568:1: '{'
            {
             before(grammarAccess.getIssueElementAccess().getLeftCurlyBracketKeyword_3()); 
            match(input,31,FOLLOW_31_in_rule__IssueElement__Group__3__Impl3264); 
             after(grammarAccess.getIssueElementAccess().getLeftCurlyBracketKeyword_3()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__3__Impl"


    // $ANTLR start "rule__IssueElement__Group__4"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1581:1: rule__IssueElement__Group__4 : rule__IssueElement__Group__4__Impl rule__IssueElement__Group__5 ;
    public final void rule__IssueElement__Group__4() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1585:1: ( rule__IssueElement__Group__4__Impl rule__IssueElement__Group__5 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1586:2: rule__IssueElement__Group__4__Impl rule__IssueElement__Group__5
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__4__Impl_in_rule__IssueElement__Group__43295);
            rule__IssueElement__Group__4__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__5_in_rule__IssueElement__Group__43298);
            rule__IssueElement__Group__5();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__4"


    // $ANTLR start "rule__IssueElement__Group__4__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1593:1: rule__IssueElement__Group__4__Impl : ( ( rule__IssueElement__MessageAssignment_4 )? ) ;
    public final void rule__IssueElement__Group__4__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1597:1: ( ( ( rule__IssueElement__MessageAssignment_4 )? ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1598:1: ( ( rule__IssueElement__MessageAssignment_4 )? )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1598:1: ( ( rule__IssueElement__MessageAssignment_4 )? )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1599:1: ( rule__IssueElement__MessageAssignment_4 )?
            {
             before(grammarAccess.getIssueElementAccess().getMessageAssignment_4()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1600:1: ( rule__IssueElement__MessageAssignment_4 )?
            int alt14=2;
            int LA14_0 = input.LA(1);

            if ( (LA14_0==RULE_STRING) ) {
                alt14=1;
            }
            switch (alt14) {
                case 1 :
                    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1600:2: rule__IssueElement__MessageAssignment_4
                    {
                    pushFollow(FOLLOW_rule__IssueElement__MessageAssignment_4_in_rule__IssueElement__Group__4__Impl3325);
                    rule__IssueElement__MessageAssignment_4();

                    state._fsp--;


                    }
                    break;

            }

             after(grammarAccess.getIssueElementAccess().getMessageAssignment_4()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__4__Impl"


    // $ANTLR start "rule__IssueElement__Group__5"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1610:1: rule__IssueElement__Group__5 : rule__IssueElement__Group__5__Impl rule__IssueElement__Group__6 ;
    public final void rule__IssueElement__Group__5() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1614:1: ( rule__IssueElement__Group__5__Impl rule__IssueElement__Group__6 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1615:2: rule__IssueElement__Group__5__Impl rule__IssueElement__Group__6
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__5__Impl_in_rule__IssueElement__Group__53356);
            rule__IssueElement__Group__5__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__6_in_rule__IssueElement__Group__53359);
            rule__IssueElement__Group__6();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__5"


    // $ANTLR start "rule__IssueElement__Group__5__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1622:1: rule__IssueElement__Group__5__Impl : ( ( rule__IssueElement__CategoriesAssignment_5 ) ) ;
    public final void rule__IssueElement__Group__5__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1626:1: ( ( ( rule__IssueElement__CategoriesAssignment_5 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1627:1: ( ( rule__IssueElement__CategoriesAssignment_5 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1627:1: ( ( rule__IssueElement__CategoriesAssignment_5 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1628:1: ( rule__IssueElement__CategoriesAssignment_5 )
            {
             before(grammarAccess.getIssueElementAccess().getCategoriesAssignment_5()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1629:1: ( rule__IssueElement__CategoriesAssignment_5 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1629:2: rule__IssueElement__CategoriesAssignment_5
            {
            pushFollow(FOLLOW_rule__IssueElement__CategoriesAssignment_5_in_rule__IssueElement__Group__5__Impl3386);
            rule__IssueElement__CategoriesAssignment_5();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getCategoriesAssignment_5()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__5__Impl"


    // $ANTLR start "rule__IssueElement__Group__6"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1639:1: rule__IssueElement__Group__6 : rule__IssueElement__Group__6__Impl rule__IssueElement__Group__7 ;
    public final void rule__IssueElement__Group__6() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1643:1: ( rule__IssueElement__Group__6__Impl rule__IssueElement__Group__7 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1644:2: rule__IssueElement__Group__6__Impl rule__IssueElement__Group__7
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__6__Impl_in_rule__IssueElement__Group__63416);
            rule__IssueElement__Group__6__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__7_in_rule__IssueElement__Group__63419);
            rule__IssueElement__Group__7();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__6"


    // $ANTLR start "rule__IssueElement__Group__6__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1651:1: rule__IssueElement__Group__6__Impl : ( ( rule__IssueElement__KindsAssignment_6 ) ) ;
    public final void rule__IssueElement__Group__6__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1655:1: ( ( ( rule__IssueElement__KindsAssignment_6 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1656:1: ( ( rule__IssueElement__KindsAssignment_6 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1656:1: ( ( rule__IssueElement__KindsAssignment_6 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1657:1: ( rule__IssueElement__KindsAssignment_6 )
            {
             before(grammarAccess.getIssueElementAccess().getKindsAssignment_6()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1658:1: ( rule__IssueElement__KindsAssignment_6 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1658:2: rule__IssueElement__KindsAssignment_6
            {
            pushFollow(FOLLOW_rule__IssueElement__KindsAssignment_6_in_rule__IssueElement__Group__6__Impl3446);
            rule__IssueElement__KindsAssignment_6();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getKindsAssignment_6()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__6__Impl"


    // $ANTLR start "rule__IssueElement__Group__7"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1668:1: rule__IssueElement__Group__7 : rule__IssueElement__Group__7__Impl rule__IssueElement__Group__8 ;
    public final void rule__IssueElement__Group__7() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1672:1: ( rule__IssueElement__Group__7__Impl rule__IssueElement__Group__8 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1673:2: rule__IssueElement__Group__7__Impl rule__IssueElement__Group__8
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__7__Impl_in_rule__IssueElement__Group__73476);
            rule__IssueElement__Group__7__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__8_in_rule__IssueElement__Group__73479);
            rule__IssueElement__Group__8();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__7"


    // $ANTLR start "rule__IssueElement__Group__7__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1680:1: rule__IssueElement__Group__7__Impl : ( ( rule__IssueElement__RelevanceAssignment_7 ) ) ;
    public final void rule__IssueElement__Group__7__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1684:1: ( ( ( rule__IssueElement__RelevanceAssignment_7 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1685:1: ( ( rule__IssueElement__RelevanceAssignment_7 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1685:1: ( ( rule__IssueElement__RelevanceAssignment_7 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1686:1: ( rule__IssueElement__RelevanceAssignment_7 )
            {
             before(grammarAccess.getIssueElementAccess().getRelevanceAssignment_7()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1687:1: ( rule__IssueElement__RelevanceAssignment_7 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1687:2: rule__IssueElement__RelevanceAssignment_7
            {
            pushFollow(FOLLOW_rule__IssueElement__RelevanceAssignment_7_in_rule__IssueElement__Group__7__Impl3506);
            rule__IssueElement__RelevanceAssignment_7();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getRelevanceAssignment_7()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__7__Impl"


    // $ANTLR start "rule__IssueElement__Group__8"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1697:1: rule__IssueElement__Group__8 : rule__IssueElement__Group__8__Impl rule__IssueElement__Group__9 ;
    public final void rule__IssueElement__Group__8() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1701:1: ( rule__IssueElement__Group__8__Impl rule__IssueElement__Group__9 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1702:2: rule__IssueElement__Group__8__Impl rule__IssueElement__Group__9
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__8__Impl_in_rule__IssueElement__Group__83536);
            rule__IssueElement__Group__8__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__9_in_rule__IssueElement__Group__83539);
            rule__IssueElement__Group__9();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__8"


    // $ANTLR start "rule__IssueElement__Group__8__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1709:1: rule__IssueElement__Group__8__Impl : ( ( rule__IssueElement__PackageAssignment_8 ) ) ;
    public final void rule__IssueElement__Group__8__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1713:1: ( ( ( rule__IssueElement__PackageAssignment_8 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1714:1: ( ( rule__IssueElement__PackageAssignment_8 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1714:1: ( ( rule__IssueElement__PackageAssignment_8 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1715:1: ( rule__IssueElement__PackageAssignment_8 )
            {
             before(grammarAccess.getIssueElementAccess().getPackageAssignment_8()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1716:1: ( rule__IssueElement__PackageAssignment_8 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1716:2: rule__IssueElement__PackageAssignment_8
            {
            pushFollow(FOLLOW_rule__IssueElement__PackageAssignment_8_in_rule__IssueElement__Group__8__Impl3566);
            rule__IssueElement__PackageAssignment_8();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getPackageAssignment_8()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__8__Impl"


    // $ANTLR start "rule__IssueElement__Group__9"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1726:1: rule__IssueElement__Group__9 : rule__IssueElement__Group__9__Impl rule__IssueElement__Group__10 ;
    public final void rule__IssueElement__Group__9() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1730:1: ( rule__IssueElement__Group__9__Impl rule__IssueElement__Group__10 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1731:2: rule__IssueElement__Group__9__Impl rule__IssueElement__Group__10
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__9__Impl_in_rule__IssueElement__Group__93596);
            rule__IssueElement__Group__9__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group__10_in_rule__IssueElement__Group__93599);
            rule__IssueElement__Group__10();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__9"


    // $ANTLR start "rule__IssueElement__Group__9__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1738:1: rule__IssueElement__Group__9__Impl : ( ( rule__IssueElement__ClassAssignment_9 ) ) ;
    public final void rule__IssueElement__Group__9__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1742:1: ( ( ( rule__IssueElement__ClassAssignment_9 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1743:1: ( ( rule__IssueElement__ClassAssignment_9 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1743:1: ( ( rule__IssueElement__ClassAssignment_9 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1744:1: ( rule__IssueElement__ClassAssignment_9 )
            {
             before(grammarAccess.getIssueElementAccess().getClassAssignment_9()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1745:1: ( rule__IssueElement__ClassAssignment_9 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1745:2: rule__IssueElement__ClassAssignment_9
            {
            pushFollow(FOLLOW_rule__IssueElement__ClassAssignment_9_in_rule__IssueElement__Group__9__Impl3626);
            rule__IssueElement__ClassAssignment_9();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getClassAssignment_9()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__9__Impl"


    // $ANTLR start "rule__IssueElement__Group__10"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1755:1: rule__IssueElement__Group__10 : rule__IssueElement__Group__10__Impl ;
    public final void rule__IssueElement__Group__10() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1759:1: ( rule__IssueElement__Group__10__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1760:2: rule__IssueElement__Group__10__Impl
            {
            pushFollow(FOLLOW_rule__IssueElement__Group__10__Impl_in_rule__IssueElement__Group__103656);
            rule__IssueElement__Group__10__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__10"


    // $ANTLR start "rule__IssueElement__Group__10__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1766:1: rule__IssueElement__Group__10__Impl : ( '}' ) ;
    public final void rule__IssueElement__Group__10__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1770:1: ( ( '}' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1771:1: ( '}' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1771:1: ( '}' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1772:1: '}'
            {
             before(grammarAccess.getIssueElementAccess().getRightCurlyBracketKeyword_10()); 
            match(input,32,FOLLOW_32_in_rule__IssueElement__Group__10__Impl3684); 
             after(grammarAccess.getIssueElementAccess().getRightCurlyBracketKeyword_10()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group__10__Impl"


    // $ANTLR start "rule__IssueElement__Group_1__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1807:1: rule__IssueElement__Group_1__0 : rule__IssueElement__Group_1__0__Impl rule__IssueElement__Group_1__1 ;
    public final void rule__IssueElement__Group_1__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1811:1: ( rule__IssueElement__Group_1__0__Impl rule__IssueElement__Group_1__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1812:2: rule__IssueElement__Group_1__0__Impl rule__IssueElement__Group_1__1
            {
            pushFollow(FOLLOW_rule__IssueElement__Group_1__0__Impl_in_rule__IssueElement__Group_1__03737);
            rule__IssueElement__Group_1__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueElement__Group_1__1_in_rule__IssueElement__Group_1__03740);
            rule__IssueElement__Group_1__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group_1__0"


    // $ANTLR start "rule__IssueElement__Group_1__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1819:1: rule__IssueElement__Group_1__0__Impl : ( ',' ) ;
    public final void rule__IssueElement__Group_1__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1823:1: ( ( ',' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1824:1: ( ',' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1824:1: ( ',' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1825:1: ','
            {
             before(grammarAccess.getIssueElementAccess().getCommaKeyword_1_0()); 
            match(input,15,FOLLOW_15_in_rule__IssueElement__Group_1__0__Impl3768); 
             after(grammarAccess.getIssueElementAccess().getCommaKeyword_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group_1__0__Impl"


    // $ANTLR start "rule__IssueElement__Group_1__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1838:1: rule__IssueElement__Group_1__1 : rule__IssueElement__Group_1__1__Impl ;
    public final void rule__IssueElement__Group_1__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1842:1: ( rule__IssueElement__Group_1__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1843:2: rule__IssueElement__Group_1__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueElement__Group_1__1__Impl_in_rule__IssueElement__Group_1__13799);
            rule__IssueElement__Group_1__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group_1__1"


    // $ANTLR start "rule__IssueElement__Group_1__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1849:1: rule__IssueElement__Group_1__1__Impl : ( ( rule__IssueElement__NameAssignment_1_1 ) ) ;
    public final void rule__IssueElement__Group_1__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1853:1: ( ( ( rule__IssueElement__NameAssignment_1_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1854:1: ( ( rule__IssueElement__NameAssignment_1_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1854:1: ( ( rule__IssueElement__NameAssignment_1_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1855:1: ( rule__IssueElement__NameAssignment_1_1 )
            {
             before(grammarAccess.getIssueElementAccess().getNameAssignment_1_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1856:1: ( rule__IssueElement__NameAssignment_1_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1856:2: rule__IssueElement__NameAssignment_1_1
            {
            pushFollow(FOLLOW_rule__IssueElement__NameAssignment_1_1_in_rule__IssueElement__Group_1__1__Impl3826);
            rule__IssueElement__NameAssignment_1_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueElementAccess().getNameAssignment_1_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__Group_1__1__Impl"


    // $ANTLR start "rule__IssueSuppressComment__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1870:1: rule__IssueSuppressComment__Group__0 : rule__IssueSuppressComment__Group__0__Impl rule__IssueSuppressComment__Group__1 ;
    public final void rule__IssueSuppressComment__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1874:1: ( rule__IssueSuppressComment__Group__0__Impl rule__IssueSuppressComment__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1875:2: rule__IssueSuppressComment__Group__0__Impl rule__IssueSuppressComment__Group__1
            {
            pushFollow(FOLLOW_rule__IssueSuppressComment__Group__0__Impl_in_rule__IssueSuppressComment__Group__03860);
            rule__IssueSuppressComment__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueSuppressComment__Group__1_in_rule__IssueSuppressComment__Group__03863);
            rule__IssueSuppressComment__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__Group__0"


    // $ANTLR start "rule__IssueSuppressComment__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1882:1: rule__IssueSuppressComment__Group__0__Impl : ( '[suppress=' ) ;
    public final void rule__IssueSuppressComment__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1886:1: ( ( '[suppress=' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1887:1: ( '[suppress=' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1887:1: ( '[suppress=' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1888:1: '[suppress='
            {
             before(grammarAccess.getIssueSuppressCommentAccess().getSuppressKeyword_0()); 
            match(input,33,FOLLOW_33_in_rule__IssueSuppressComment__Group__0__Impl3891); 
             after(grammarAccess.getIssueSuppressCommentAccess().getSuppressKeyword_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__Group__0__Impl"


    // $ANTLR start "rule__IssueSuppressComment__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1901:1: rule__IssueSuppressComment__Group__1 : rule__IssueSuppressComment__Group__1__Impl rule__IssueSuppressComment__Group__2 ;
    public final void rule__IssueSuppressComment__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1905:1: ( rule__IssueSuppressComment__Group__1__Impl rule__IssueSuppressComment__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1906:2: rule__IssueSuppressComment__Group__1__Impl rule__IssueSuppressComment__Group__2
            {
            pushFollow(FOLLOW_rule__IssueSuppressComment__Group__1__Impl_in_rule__IssueSuppressComment__Group__13922);
            rule__IssueSuppressComment__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueSuppressComment__Group__2_in_rule__IssueSuppressComment__Group__13925);
            rule__IssueSuppressComment__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__Group__1"


    // $ANTLR start "rule__IssueSuppressComment__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1913:1: rule__IssueSuppressComment__Group__1__Impl : ( ( rule__IssueSuppressComment__ValueAssignment_1 ) ) ;
    public final void rule__IssueSuppressComment__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1917:1: ( ( ( rule__IssueSuppressComment__ValueAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1918:1: ( ( rule__IssueSuppressComment__ValueAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1918:1: ( ( rule__IssueSuppressComment__ValueAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1919:1: ( rule__IssueSuppressComment__ValueAssignment_1 )
            {
             before(grammarAccess.getIssueSuppressCommentAccess().getValueAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1920:1: ( rule__IssueSuppressComment__ValueAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1920:2: rule__IssueSuppressComment__ValueAssignment_1
            {
            pushFollow(FOLLOW_rule__IssueSuppressComment__ValueAssignment_1_in_rule__IssueSuppressComment__Group__1__Impl3952);
            rule__IssueSuppressComment__ValueAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueSuppressCommentAccess().getValueAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__Group__1__Impl"


    // $ANTLR start "rule__IssueSuppressComment__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1930:1: rule__IssueSuppressComment__Group__2 : rule__IssueSuppressComment__Group__2__Impl ;
    public final void rule__IssueSuppressComment__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1934:1: ( rule__IssueSuppressComment__Group__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1935:2: rule__IssueSuppressComment__Group__2__Impl
            {
            pushFollow(FOLLOW_rule__IssueSuppressComment__Group__2__Impl_in_rule__IssueSuppressComment__Group__23982);
            rule__IssueSuppressComment__Group__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__Group__2"


    // $ANTLR start "rule__IssueSuppressComment__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1941:1: rule__IssueSuppressComment__Group__2__Impl : ( ']' ) ;
    public final void rule__IssueSuppressComment__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1945:1: ( ( ']' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1946:1: ( ']' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1946:1: ( ']' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1947:1: ']'
            {
             before(grammarAccess.getIssueSuppressCommentAccess().getRightSquareBracketKeyword_2()); 
            match(input,34,FOLLOW_34_in_rule__IssueSuppressComment__Group__2__Impl4010); 
             after(grammarAccess.getIssueSuppressCommentAccess().getRightSquareBracketKeyword_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__Group__2__Impl"


    // $ANTLR start "rule__IssueCategoryElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1966:1: rule__IssueCategoryElement__Group__0 : rule__IssueCategoryElement__Group__0__Impl rule__IssueCategoryElement__Group__1 ;
    public final void rule__IssueCategoryElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1970:1: ( rule__IssueCategoryElement__Group__0__Impl rule__IssueCategoryElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1971:2: rule__IssueCategoryElement__Group__0__Impl rule__IssueCategoryElement__Group__1
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__Group__0__Impl_in_rule__IssueCategoryElement__Group__04047);
            rule__IssueCategoryElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueCategoryElement__Group__1_in_rule__IssueCategoryElement__Group__04050);
            rule__IssueCategoryElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group__0"


    // $ANTLR start "rule__IssueCategoryElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1978:1: rule__IssueCategoryElement__Group__0__Impl : ( ( rule__IssueCategoryElement__NameAssignment_0 ) ) ;
    public final void rule__IssueCategoryElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1982:1: ( ( ( rule__IssueCategoryElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1983:1: ( ( rule__IssueCategoryElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1983:1: ( ( rule__IssueCategoryElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1984:1: ( rule__IssueCategoryElement__NameAssignment_0 )
            {
             before(grammarAccess.getIssueCategoryElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1985:1: ( rule__IssueCategoryElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1985:2: rule__IssueCategoryElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__NameAssignment_0_in_rule__IssueCategoryElement__Group__0__Impl4077);
            rule__IssueCategoryElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getIssueCategoryElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group__0__Impl"


    // $ANTLR start "rule__IssueCategoryElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1995:1: rule__IssueCategoryElement__Group__1 : rule__IssueCategoryElement__Group__1__Impl rule__IssueCategoryElement__Group__2 ;
    public final void rule__IssueCategoryElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:1999:1: ( rule__IssueCategoryElement__Group__1__Impl rule__IssueCategoryElement__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2000:2: rule__IssueCategoryElement__Group__1__Impl rule__IssueCategoryElement__Group__2
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__Group__1__Impl_in_rule__IssueCategoryElement__Group__14107);
            rule__IssueCategoryElement__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueCategoryElement__Group__2_in_rule__IssueCategoryElement__Group__14110);
            rule__IssueCategoryElement__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group__1"


    // $ANTLR start "rule__IssueCategoryElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2007:1: rule__IssueCategoryElement__Group__1__Impl : ( ( rule__IssueCategoryElement__ElementsAssignment_1 ) ) ;
    public final void rule__IssueCategoryElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2011:1: ( ( ( rule__IssueCategoryElement__ElementsAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2012:1: ( ( rule__IssueCategoryElement__ElementsAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2012:1: ( ( rule__IssueCategoryElement__ElementsAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2013:1: ( rule__IssueCategoryElement__ElementsAssignment_1 )
            {
             before(grammarAccess.getIssueCategoryElementAccess().getElementsAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2014:1: ( rule__IssueCategoryElement__ElementsAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2014:2: rule__IssueCategoryElement__ElementsAssignment_1
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__ElementsAssignment_1_in_rule__IssueCategoryElement__Group__1__Impl4137);
            rule__IssueCategoryElement__ElementsAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueCategoryElementAccess().getElementsAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group__1__Impl"


    // $ANTLR start "rule__IssueCategoryElement__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2024:1: rule__IssueCategoryElement__Group__2 : rule__IssueCategoryElement__Group__2__Impl ;
    public final void rule__IssueCategoryElement__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2028:1: ( rule__IssueCategoryElement__Group__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2029:2: rule__IssueCategoryElement__Group__2__Impl
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__Group__2__Impl_in_rule__IssueCategoryElement__Group__24167);
            rule__IssueCategoryElement__Group__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group__2"


    // $ANTLR start "rule__IssueCategoryElement__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2035:1: rule__IssueCategoryElement__Group__2__Impl : ( ( rule__IssueCategoryElement__Group_2__0 )* ) ;
    public final void rule__IssueCategoryElement__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2039:1: ( ( ( rule__IssueCategoryElement__Group_2__0 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2040:1: ( ( rule__IssueCategoryElement__Group_2__0 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2040:1: ( ( rule__IssueCategoryElement__Group_2__0 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2041:1: ( rule__IssueCategoryElement__Group_2__0 )*
            {
             before(grammarAccess.getIssueCategoryElementAccess().getGroup_2()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2042:1: ( rule__IssueCategoryElement__Group_2__0 )*
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( (LA15_0==15) ) {
                    alt15=1;
                }


                switch (alt15) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2042:2: rule__IssueCategoryElement__Group_2__0
            	    {
            	    pushFollow(FOLLOW_rule__IssueCategoryElement__Group_2__0_in_rule__IssueCategoryElement__Group__2__Impl4194);
            	    rule__IssueCategoryElement__Group_2__0();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop15;
                }
            } while (true);

             after(grammarAccess.getIssueCategoryElementAccess().getGroup_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group__2__Impl"


    // $ANTLR start "rule__IssueCategoryElement__Group_2__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2058:1: rule__IssueCategoryElement__Group_2__0 : rule__IssueCategoryElement__Group_2__0__Impl rule__IssueCategoryElement__Group_2__1 ;
    public final void rule__IssueCategoryElement__Group_2__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2062:1: ( rule__IssueCategoryElement__Group_2__0__Impl rule__IssueCategoryElement__Group_2__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2063:2: rule__IssueCategoryElement__Group_2__0__Impl rule__IssueCategoryElement__Group_2__1
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__Group_2__0__Impl_in_rule__IssueCategoryElement__Group_2__04231);
            rule__IssueCategoryElement__Group_2__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueCategoryElement__Group_2__1_in_rule__IssueCategoryElement__Group_2__04234);
            rule__IssueCategoryElement__Group_2__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group_2__0"


    // $ANTLR start "rule__IssueCategoryElement__Group_2__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2070:1: rule__IssueCategoryElement__Group_2__0__Impl : ( ',' ) ;
    public final void rule__IssueCategoryElement__Group_2__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2074:1: ( ( ',' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2075:1: ( ',' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2075:1: ( ',' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2076:1: ','
            {
             before(grammarAccess.getIssueCategoryElementAccess().getCommaKeyword_2_0()); 
            match(input,15,FOLLOW_15_in_rule__IssueCategoryElement__Group_2__0__Impl4262); 
             after(grammarAccess.getIssueCategoryElementAccess().getCommaKeyword_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group_2__0__Impl"


    // $ANTLR start "rule__IssueCategoryElement__Group_2__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2089:1: rule__IssueCategoryElement__Group_2__1 : rule__IssueCategoryElement__Group_2__1__Impl ;
    public final void rule__IssueCategoryElement__Group_2__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2093:1: ( rule__IssueCategoryElement__Group_2__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2094:2: rule__IssueCategoryElement__Group_2__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__Group_2__1__Impl_in_rule__IssueCategoryElement__Group_2__14293);
            rule__IssueCategoryElement__Group_2__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group_2__1"


    // $ANTLR start "rule__IssueCategoryElement__Group_2__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2100:1: rule__IssueCategoryElement__Group_2__1__Impl : ( ( rule__IssueCategoryElement__ElementsAssignment_2_1 ) ) ;
    public final void rule__IssueCategoryElement__Group_2__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2104:1: ( ( ( rule__IssueCategoryElement__ElementsAssignment_2_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2105:1: ( ( rule__IssueCategoryElement__ElementsAssignment_2_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2105:1: ( ( rule__IssueCategoryElement__ElementsAssignment_2_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2106:1: ( rule__IssueCategoryElement__ElementsAssignment_2_1 )
            {
             before(grammarAccess.getIssueCategoryElementAccess().getElementsAssignment_2_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2107:1: ( rule__IssueCategoryElement__ElementsAssignment_2_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2107:2: rule__IssueCategoryElement__ElementsAssignment_2_1
            {
            pushFollow(FOLLOW_rule__IssueCategoryElement__ElementsAssignment_2_1_in_rule__IssueCategoryElement__Group_2__1__Impl4320);
            rule__IssueCategoryElement__ElementsAssignment_2_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueCategoryElementAccess().getElementsAssignment_2_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__Group_2__1__Impl"


    // $ANTLR start "rule__IssueKindElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2121:1: rule__IssueKindElement__Group__0 : rule__IssueKindElement__Group__0__Impl rule__IssueKindElement__Group__1 ;
    public final void rule__IssueKindElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2125:1: ( rule__IssueKindElement__Group__0__Impl rule__IssueKindElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2126:2: rule__IssueKindElement__Group__0__Impl rule__IssueKindElement__Group__1
            {
            pushFollow(FOLLOW_rule__IssueKindElement__Group__0__Impl_in_rule__IssueKindElement__Group__04354);
            rule__IssueKindElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueKindElement__Group__1_in_rule__IssueKindElement__Group__04357);
            rule__IssueKindElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group__0"


    // $ANTLR start "rule__IssueKindElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2133:1: rule__IssueKindElement__Group__0__Impl : ( ( rule__IssueKindElement__NameAssignment_0 ) ) ;
    public final void rule__IssueKindElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2137:1: ( ( ( rule__IssueKindElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2138:1: ( ( rule__IssueKindElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2138:1: ( ( rule__IssueKindElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2139:1: ( rule__IssueKindElement__NameAssignment_0 )
            {
             before(grammarAccess.getIssueKindElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2140:1: ( rule__IssueKindElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2140:2: rule__IssueKindElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__IssueKindElement__NameAssignment_0_in_rule__IssueKindElement__Group__0__Impl4384);
            rule__IssueKindElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getIssueKindElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group__0__Impl"


    // $ANTLR start "rule__IssueKindElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2150:1: rule__IssueKindElement__Group__1 : rule__IssueKindElement__Group__1__Impl rule__IssueKindElement__Group__2 ;
    public final void rule__IssueKindElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2154:1: ( rule__IssueKindElement__Group__1__Impl rule__IssueKindElement__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2155:2: rule__IssueKindElement__Group__1__Impl rule__IssueKindElement__Group__2
            {
            pushFollow(FOLLOW_rule__IssueKindElement__Group__1__Impl_in_rule__IssueKindElement__Group__14414);
            rule__IssueKindElement__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueKindElement__Group__2_in_rule__IssueKindElement__Group__14417);
            rule__IssueKindElement__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group__1"


    // $ANTLR start "rule__IssueKindElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2162:1: rule__IssueKindElement__Group__1__Impl : ( ( rule__IssueKindElement__ElementsAssignment_1 ) ) ;
    public final void rule__IssueKindElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2166:1: ( ( ( rule__IssueKindElement__ElementsAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2167:1: ( ( rule__IssueKindElement__ElementsAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2167:1: ( ( rule__IssueKindElement__ElementsAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2168:1: ( rule__IssueKindElement__ElementsAssignment_1 )
            {
             before(grammarAccess.getIssueKindElementAccess().getElementsAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2169:1: ( rule__IssueKindElement__ElementsAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2169:2: rule__IssueKindElement__ElementsAssignment_1
            {
            pushFollow(FOLLOW_rule__IssueKindElement__ElementsAssignment_1_in_rule__IssueKindElement__Group__1__Impl4444);
            rule__IssueKindElement__ElementsAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueKindElementAccess().getElementsAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group__1__Impl"


    // $ANTLR start "rule__IssueKindElement__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2179:1: rule__IssueKindElement__Group__2 : rule__IssueKindElement__Group__2__Impl ;
    public final void rule__IssueKindElement__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2183:1: ( rule__IssueKindElement__Group__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2184:2: rule__IssueKindElement__Group__2__Impl
            {
            pushFollow(FOLLOW_rule__IssueKindElement__Group__2__Impl_in_rule__IssueKindElement__Group__24474);
            rule__IssueKindElement__Group__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group__2"


    // $ANTLR start "rule__IssueKindElement__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2190:1: rule__IssueKindElement__Group__2__Impl : ( ( rule__IssueKindElement__Group_2__0 )* ) ;
    public final void rule__IssueKindElement__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2194:1: ( ( ( rule__IssueKindElement__Group_2__0 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2195:1: ( ( rule__IssueKindElement__Group_2__0 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2195:1: ( ( rule__IssueKindElement__Group_2__0 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2196:1: ( rule__IssueKindElement__Group_2__0 )*
            {
             before(grammarAccess.getIssueKindElementAccess().getGroup_2()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2197:1: ( rule__IssueKindElement__Group_2__0 )*
            loop16:
            do {
                int alt16=2;
                int LA16_0 = input.LA(1);

                if ( (LA16_0==15) ) {
                    alt16=1;
                }


                switch (alt16) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2197:2: rule__IssueKindElement__Group_2__0
            	    {
            	    pushFollow(FOLLOW_rule__IssueKindElement__Group_2__0_in_rule__IssueKindElement__Group__2__Impl4501);
            	    rule__IssueKindElement__Group_2__0();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop16;
                }
            } while (true);

             after(grammarAccess.getIssueKindElementAccess().getGroup_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group__2__Impl"


    // $ANTLR start "rule__IssueKindElement__Group_2__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2213:1: rule__IssueKindElement__Group_2__0 : rule__IssueKindElement__Group_2__0__Impl rule__IssueKindElement__Group_2__1 ;
    public final void rule__IssueKindElement__Group_2__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2217:1: ( rule__IssueKindElement__Group_2__0__Impl rule__IssueKindElement__Group_2__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2218:2: rule__IssueKindElement__Group_2__0__Impl rule__IssueKindElement__Group_2__1
            {
            pushFollow(FOLLOW_rule__IssueKindElement__Group_2__0__Impl_in_rule__IssueKindElement__Group_2__04538);
            rule__IssueKindElement__Group_2__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueKindElement__Group_2__1_in_rule__IssueKindElement__Group_2__04541);
            rule__IssueKindElement__Group_2__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group_2__0"


    // $ANTLR start "rule__IssueKindElement__Group_2__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2225:1: rule__IssueKindElement__Group_2__0__Impl : ( ',' ) ;
    public final void rule__IssueKindElement__Group_2__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2229:1: ( ( ',' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2230:1: ( ',' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2230:1: ( ',' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2231:1: ','
            {
             before(grammarAccess.getIssueKindElementAccess().getCommaKeyword_2_0()); 
            match(input,15,FOLLOW_15_in_rule__IssueKindElement__Group_2__0__Impl4569); 
             after(grammarAccess.getIssueKindElementAccess().getCommaKeyword_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group_2__0__Impl"


    // $ANTLR start "rule__IssueKindElement__Group_2__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2244:1: rule__IssueKindElement__Group_2__1 : rule__IssueKindElement__Group_2__1__Impl ;
    public final void rule__IssueKindElement__Group_2__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2248:1: ( rule__IssueKindElement__Group_2__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2249:2: rule__IssueKindElement__Group_2__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueKindElement__Group_2__1__Impl_in_rule__IssueKindElement__Group_2__14600);
            rule__IssueKindElement__Group_2__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group_2__1"


    // $ANTLR start "rule__IssueKindElement__Group_2__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2255:1: rule__IssueKindElement__Group_2__1__Impl : ( ( rule__IssueKindElement__ElementsAssignment_2_1 ) ) ;
    public final void rule__IssueKindElement__Group_2__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2259:1: ( ( ( rule__IssueKindElement__ElementsAssignment_2_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2260:1: ( ( rule__IssueKindElement__ElementsAssignment_2_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2260:1: ( ( rule__IssueKindElement__ElementsAssignment_2_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2261:1: ( rule__IssueKindElement__ElementsAssignment_2_1 )
            {
             before(grammarAccess.getIssueKindElementAccess().getElementsAssignment_2_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2262:1: ( rule__IssueKindElement__ElementsAssignment_2_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2262:2: rule__IssueKindElement__ElementsAssignment_2_1
            {
            pushFollow(FOLLOW_rule__IssueKindElement__ElementsAssignment_2_1_in_rule__IssueKindElement__Group_2__1__Impl4627);
            rule__IssueKindElement__ElementsAssignment_2_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueKindElementAccess().getElementsAssignment_2_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__Group_2__1__Impl"


    // $ANTLR start "rule__IssueRelevanceElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2276:1: rule__IssueRelevanceElement__Group__0 : rule__IssueRelevanceElement__Group__0__Impl rule__IssueRelevanceElement__Group__1 ;
    public final void rule__IssueRelevanceElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2280:1: ( rule__IssueRelevanceElement__Group__0__Impl rule__IssueRelevanceElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2281:2: rule__IssueRelevanceElement__Group__0__Impl rule__IssueRelevanceElement__Group__1
            {
            pushFollow(FOLLOW_rule__IssueRelevanceElement__Group__0__Impl_in_rule__IssueRelevanceElement__Group__04661);
            rule__IssueRelevanceElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueRelevanceElement__Group__1_in_rule__IssueRelevanceElement__Group__04664);
            rule__IssueRelevanceElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueRelevanceElement__Group__0"


    // $ANTLR start "rule__IssueRelevanceElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2288:1: rule__IssueRelevanceElement__Group__0__Impl : ( ( rule__IssueRelevanceElement__NameAssignment_0 ) ) ;
    public final void rule__IssueRelevanceElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2292:1: ( ( ( rule__IssueRelevanceElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2293:1: ( ( rule__IssueRelevanceElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2293:1: ( ( rule__IssueRelevanceElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2294:1: ( rule__IssueRelevanceElement__NameAssignment_0 )
            {
             before(grammarAccess.getIssueRelevanceElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2295:1: ( rule__IssueRelevanceElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2295:2: rule__IssueRelevanceElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__IssueRelevanceElement__NameAssignment_0_in_rule__IssueRelevanceElement__Group__0__Impl4691);
            rule__IssueRelevanceElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getIssueRelevanceElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueRelevanceElement__Group__0__Impl"


    // $ANTLR start "rule__IssueRelevanceElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2305:1: rule__IssueRelevanceElement__Group__1 : rule__IssueRelevanceElement__Group__1__Impl ;
    public final void rule__IssueRelevanceElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2309:1: ( rule__IssueRelevanceElement__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2310:2: rule__IssueRelevanceElement__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueRelevanceElement__Group__1__Impl_in_rule__IssueRelevanceElement__Group__14721);
            rule__IssueRelevanceElement__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueRelevanceElement__Group__1"


    // $ANTLR start "rule__IssueRelevanceElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2316:1: rule__IssueRelevanceElement__Group__1__Impl : ( ( rule__IssueRelevanceElement__RelevanceAssignment_1 ) ) ;
    public final void rule__IssueRelevanceElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2320:1: ( ( ( rule__IssueRelevanceElement__RelevanceAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2321:1: ( ( rule__IssueRelevanceElement__RelevanceAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2321:1: ( ( rule__IssueRelevanceElement__RelevanceAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2322:1: ( rule__IssueRelevanceElement__RelevanceAssignment_1 )
            {
             before(grammarAccess.getIssueRelevanceElementAccess().getRelevanceAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2323:1: ( rule__IssueRelevanceElement__RelevanceAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2323:2: rule__IssueRelevanceElement__RelevanceAssignment_1
            {
            pushFollow(FOLLOW_rule__IssueRelevanceElement__RelevanceAssignment_1_in_rule__IssueRelevanceElement__Group__1__Impl4748);
            rule__IssueRelevanceElement__RelevanceAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueRelevanceElementAccess().getRelevanceAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueRelevanceElement__Group__1__Impl"


    // $ANTLR start "rule__IssuePackageElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2337:1: rule__IssuePackageElement__Group__0 : rule__IssuePackageElement__Group__0__Impl rule__IssuePackageElement__Group__1 ;
    public final void rule__IssuePackageElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2341:1: ( rule__IssuePackageElement__Group__0__Impl rule__IssuePackageElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2342:2: rule__IssuePackageElement__Group__0__Impl rule__IssuePackageElement__Group__1
            {
            pushFollow(FOLLOW_rule__IssuePackageElement__Group__0__Impl_in_rule__IssuePackageElement__Group__04782);
            rule__IssuePackageElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssuePackageElement__Group__1_in_rule__IssuePackageElement__Group__04785);
            rule__IssuePackageElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuePackageElement__Group__0"


    // $ANTLR start "rule__IssuePackageElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2349:1: rule__IssuePackageElement__Group__0__Impl : ( ( rule__IssuePackageElement__NameAssignment_0 ) ) ;
    public final void rule__IssuePackageElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2353:1: ( ( ( rule__IssuePackageElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2354:1: ( ( rule__IssuePackageElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2354:1: ( ( rule__IssuePackageElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2355:1: ( rule__IssuePackageElement__NameAssignment_0 )
            {
             before(grammarAccess.getIssuePackageElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2356:1: ( rule__IssuePackageElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2356:2: rule__IssuePackageElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__IssuePackageElement__NameAssignment_0_in_rule__IssuePackageElement__Group__0__Impl4812);
            rule__IssuePackageElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getIssuePackageElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuePackageElement__Group__0__Impl"


    // $ANTLR start "rule__IssuePackageElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2366:1: rule__IssuePackageElement__Group__1 : rule__IssuePackageElement__Group__1__Impl ;
    public final void rule__IssuePackageElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2370:1: ( rule__IssuePackageElement__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2371:2: rule__IssuePackageElement__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__IssuePackageElement__Group__1__Impl_in_rule__IssuePackageElement__Group__14842);
            rule__IssuePackageElement__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuePackageElement__Group__1"


    // $ANTLR start "rule__IssuePackageElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2377:1: rule__IssuePackageElement__Group__1__Impl : ( ( rule__IssuePackageElement__PackageAssignment_1 ) ) ;
    public final void rule__IssuePackageElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2381:1: ( ( ( rule__IssuePackageElement__PackageAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2382:1: ( ( rule__IssuePackageElement__PackageAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2382:1: ( ( rule__IssuePackageElement__PackageAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2383:1: ( rule__IssuePackageElement__PackageAssignment_1 )
            {
             before(grammarAccess.getIssuePackageElementAccess().getPackageAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2384:1: ( rule__IssuePackageElement__PackageAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2384:2: rule__IssuePackageElement__PackageAssignment_1
            {
            pushFollow(FOLLOW_rule__IssuePackageElement__PackageAssignment_1_in_rule__IssuePackageElement__Group__1__Impl4869);
            rule__IssuePackageElement__PackageAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getIssuePackageElementAccess().getPackageAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuePackageElement__Group__1__Impl"


    // $ANTLR start "rule__IssueClassElement__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2398:1: rule__IssueClassElement__Group__0 : rule__IssueClassElement__Group__0__Impl rule__IssueClassElement__Group__1 ;
    public final void rule__IssueClassElement__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2402:1: ( rule__IssueClassElement__Group__0__Impl rule__IssueClassElement__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2403:2: rule__IssueClassElement__Group__0__Impl rule__IssueClassElement__Group__1
            {
            pushFollow(FOLLOW_rule__IssueClassElement__Group__0__Impl_in_rule__IssueClassElement__Group__04903);
            rule__IssueClassElement__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueClassElement__Group__1_in_rule__IssueClassElement__Group__04906);
            rule__IssueClassElement__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClassElement__Group__0"


    // $ANTLR start "rule__IssueClassElement__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2410:1: rule__IssueClassElement__Group__0__Impl : ( ( rule__IssueClassElement__NameAssignment_0 ) ) ;
    public final void rule__IssueClassElement__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2414:1: ( ( ( rule__IssueClassElement__NameAssignment_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2415:1: ( ( rule__IssueClassElement__NameAssignment_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2415:1: ( ( rule__IssueClassElement__NameAssignment_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2416:1: ( rule__IssueClassElement__NameAssignment_0 )
            {
             before(grammarAccess.getIssueClassElementAccess().getNameAssignment_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2417:1: ( rule__IssueClassElement__NameAssignment_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2417:2: rule__IssueClassElement__NameAssignment_0
            {
            pushFollow(FOLLOW_rule__IssueClassElement__NameAssignment_0_in_rule__IssueClassElement__Group__0__Impl4933);
            rule__IssueClassElement__NameAssignment_0();

            state._fsp--;


            }

             after(grammarAccess.getIssueClassElementAccess().getNameAssignment_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClassElement__Group__0__Impl"


    // $ANTLR start "rule__IssueClassElement__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2427:1: rule__IssueClassElement__Group__1 : rule__IssueClassElement__Group__1__Impl ;
    public final void rule__IssueClassElement__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2431:1: ( rule__IssueClassElement__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2432:2: rule__IssueClassElement__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueClassElement__Group__1__Impl_in_rule__IssueClassElement__Group__14963);
            rule__IssueClassElement__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClassElement__Group__1"


    // $ANTLR start "rule__IssueClassElement__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2438:1: rule__IssueClassElement__Group__1__Impl : ( ( rule__IssueClassElement__ClassAssignment_1 ) ) ;
    public final void rule__IssueClassElement__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2442:1: ( ( ( rule__IssueClassElement__ClassAssignment_1 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2443:1: ( ( rule__IssueClassElement__ClassAssignment_1 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2443:1: ( ( rule__IssueClassElement__ClassAssignment_1 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2444:1: ( rule__IssueClassElement__ClassAssignment_1 )
            {
             before(grammarAccess.getIssueClassElementAccess().getClassAssignment_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2445:1: ( rule__IssueClassElement__ClassAssignment_1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2445:2: rule__IssueClassElement__ClassAssignment_1
            {
            pushFollow(FOLLOW_rule__IssueClassElement__ClassAssignment_1_in_rule__IssueClassElement__Group__1__Impl4990);
            rule__IssueClassElement__ClassAssignment_1();

            state._fsp--;


            }

             after(grammarAccess.getIssueClassElementAccess().getClassAssignment_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClassElement__Group__1__Impl"


    // $ANTLR start "rule__IssueClass__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2459:1: rule__IssueClass__Group__0 : rule__IssueClass__Group__0__Impl rule__IssueClass__Group__1 ;
    public final void rule__IssueClass__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2463:1: ( rule__IssueClass__Group__0__Impl rule__IssueClass__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2464:2: rule__IssueClass__Group__0__Impl rule__IssueClass__Group__1
            {
            pushFollow(FOLLOW_rule__IssueClass__Group__0__Impl_in_rule__IssueClass__Group__05024);
            rule__IssueClass__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueClass__Group__1_in_rule__IssueClass__Group__05027);
            rule__IssueClass__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group__0"


    // $ANTLR start "rule__IssueClass__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2471:1: rule__IssueClass__Group__0__Impl : ( RULE_ID ) ;
    public final void rule__IssueClass__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2475:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2476:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2476:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2477:1: RULE_ID
            {
             before(grammarAccess.getIssueClassAccess().getIDTerminalRuleCall_0()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__IssueClass__Group__0__Impl5054); 
             after(grammarAccess.getIssueClassAccess().getIDTerminalRuleCall_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group__0__Impl"


    // $ANTLR start "rule__IssueClass__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2488:1: rule__IssueClass__Group__1 : rule__IssueClass__Group__1__Impl rule__IssueClass__Group__2 ;
    public final void rule__IssueClass__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2492:1: ( rule__IssueClass__Group__1__Impl rule__IssueClass__Group__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2493:2: rule__IssueClass__Group__1__Impl rule__IssueClass__Group__2
            {
            pushFollow(FOLLOW_rule__IssueClass__Group__1__Impl_in_rule__IssueClass__Group__15083);
            rule__IssueClass__Group__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueClass__Group__2_in_rule__IssueClass__Group__15086);
            rule__IssueClass__Group__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group__1"


    // $ANTLR start "rule__IssueClass__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2500:1: rule__IssueClass__Group__1__Impl : ( ( rule__IssueClass__Group_1__0 )* ) ;
    public final void rule__IssueClass__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2504:1: ( ( ( rule__IssueClass__Group_1__0 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2505:1: ( ( rule__IssueClass__Group_1__0 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2505:1: ( ( rule__IssueClass__Group_1__0 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2506:1: ( rule__IssueClass__Group_1__0 )*
            {
             before(grammarAccess.getIssueClassAccess().getGroup_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2507:1: ( rule__IssueClass__Group_1__0 )*
            loop17:
            do {
                int alt17=2;
                int LA17_0 = input.LA(1);

                if ( (LA17_0==35) ) {
                    int LA17_1 = input.LA(2);

                    if ( (LA17_1==RULE_ID) ) {
                        alt17=1;
                    }


                }


                switch (alt17) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2507:2: rule__IssueClass__Group_1__0
            	    {
            	    pushFollow(FOLLOW_rule__IssueClass__Group_1__0_in_rule__IssueClass__Group__1__Impl5113);
            	    rule__IssueClass__Group_1__0();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop17;
                }
            } while (true);

             after(grammarAccess.getIssueClassAccess().getGroup_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group__1__Impl"


    // $ANTLR start "rule__IssueClass__Group__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2517:1: rule__IssueClass__Group__2 : rule__IssueClass__Group__2__Impl ;
    public final void rule__IssueClass__Group__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2521:1: ( rule__IssueClass__Group__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2522:2: rule__IssueClass__Group__2__Impl
            {
            pushFollow(FOLLOW_rule__IssueClass__Group__2__Impl_in_rule__IssueClass__Group__25144);
            rule__IssueClass__Group__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group__2"


    // $ANTLR start "rule__IssueClass__Group__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2528:1: rule__IssueClass__Group__2__Impl : ( ( rule__IssueClass__Group_2__0 )* ) ;
    public final void rule__IssueClass__Group__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2532:1: ( ( ( rule__IssueClass__Group_2__0 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2533:1: ( ( rule__IssueClass__Group_2__0 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2533:1: ( ( rule__IssueClass__Group_2__0 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2534:1: ( rule__IssueClass__Group_2__0 )*
            {
             before(grammarAccess.getIssueClassAccess().getGroup_2()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2535:1: ( rule__IssueClass__Group_2__0 )*
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);

                if ( (LA18_0==35) ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2535:2: rule__IssueClass__Group_2__0
            	    {
            	    pushFollow(FOLLOW_rule__IssueClass__Group_2__0_in_rule__IssueClass__Group__2__Impl5171);
            	    rule__IssueClass__Group_2__0();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop18;
                }
            } while (true);

             after(grammarAccess.getIssueClassAccess().getGroup_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group__2__Impl"


    // $ANTLR start "rule__IssueClass__Group_1__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2551:1: rule__IssueClass__Group_1__0 : rule__IssueClass__Group_1__0__Impl rule__IssueClass__Group_1__1 ;
    public final void rule__IssueClass__Group_1__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2555:1: ( rule__IssueClass__Group_1__0__Impl rule__IssueClass__Group_1__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2556:2: rule__IssueClass__Group_1__0__Impl rule__IssueClass__Group_1__1
            {
            pushFollow(FOLLOW_rule__IssueClass__Group_1__0__Impl_in_rule__IssueClass__Group_1__05208);
            rule__IssueClass__Group_1__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueClass__Group_1__1_in_rule__IssueClass__Group_1__05211);
            rule__IssueClass__Group_1__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_1__0"


    // $ANTLR start "rule__IssueClass__Group_1__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2563:1: rule__IssueClass__Group_1__0__Impl : ( '$' ) ;
    public final void rule__IssueClass__Group_1__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2567:1: ( ( '$' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2568:1: ( '$' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2568:1: ( '$' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2569:1: '$'
            {
             before(grammarAccess.getIssueClassAccess().getDollarSignKeyword_1_0()); 
            match(input,35,FOLLOW_35_in_rule__IssueClass__Group_1__0__Impl5239); 
             after(grammarAccess.getIssueClassAccess().getDollarSignKeyword_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_1__0__Impl"


    // $ANTLR start "rule__IssueClass__Group_1__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2582:1: rule__IssueClass__Group_1__1 : rule__IssueClass__Group_1__1__Impl ;
    public final void rule__IssueClass__Group_1__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2586:1: ( rule__IssueClass__Group_1__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2587:2: rule__IssueClass__Group_1__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueClass__Group_1__1__Impl_in_rule__IssueClass__Group_1__15270);
            rule__IssueClass__Group_1__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_1__1"


    // $ANTLR start "rule__IssueClass__Group_1__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2593:1: rule__IssueClass__Group_1__1__Impl : ( RULE_ID ) ;
    public final void rule__IssueClass__Group_1__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2597:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2598:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2598:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2599:1: RULE_ID
            {
             before(grammarAccess.getIssueClassAccess().getIDTerminalRuleCall_1_1()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__IssueClass__Group_1__1__Impl5297); 
             after(grammarAccess.getIssueClassAccess().getIDTerminalRuleCall_1_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_1__1__Impl"


    // $ANTLR start "rule__IssueClass__Group_2__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2614:1: rule__IssueClass__Group_2__0 : rule__IssueClass__Group_2__0__Impl rule__IssueClass__Group_2__1 ;
    public final void rule__IssueClass__Group_2__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2618:1: ( rule__IssueClass__Group_2__0__Impl rule__IssueClass__Group_2__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2619:2: rule__IssueClass__Group_2__0__Impl rule__IssueClass__Group_2__1
            {
            pushFollow(FOLLOW_rule__IssueClass__Group_2__0__Impl_in_rule__IssueClass__Group_2__05330);
            rule__IssueClass__Group_2__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__IssueClass__Group_2__1_in_rule__IssueClass__Group_2__05333);
            rule__IssueClass__Group_2__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_2__0"


    // $ANTLR start "rule__IssueClass__Group_2__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2626:1: rule__IssueClass__Group_2__0__Impl : ( '$' ) ;
    public final void rule__IssueClass__Group_2__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2630:1: ( ( '$' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2631:1: ( '$' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2631:1: ( '$' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2632:1: '$'
            {
             before(grammarAccess.getIssueClassAccess().getDollarSignKeyword_2_0()); 
            match(input,35,FOLLOW_35_in_rule__IssueClass__Group_2__0__Impl5361); 
             after(grammarAccess.getIssueClassAccess().getDollarSignKeyword_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_2__0__Impl"


    // $ANTLR start "rule__IssueClass__Group_2__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2645:1: rule__IssueClass__Group_2__1 : rule__IssueClass__Group_2__1__Impl ;
    public final void rule__IssueClass__Group_2__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2649:1: ( rule__IssueClass__Group_2__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2650:2: rule__IssueClass__Group_2__1__Impl
            {
            pushFollow(FOLLOW_rule__IssueClass__Group_2__1__Impl_in_rule__IssueClass__Group_2__15392);
            rule__IssueClass__Group_2__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_2__1"


    // $ANTLR start "rule__IssueClass__Group_2__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2656:1: rule__IssueClass__Group_2__1__Impl : ( RULE_INT ) ;
    public final void rule__IssueClass__Group_2__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2660:1: ( ( RULE_INT ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2661:1: ( RULE_INT )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2661:1: ( RULE_INT )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2662:1: RULE_INT
            {
             before(grammarAccess.getIssueClassAccess().getINTTerminalRuleCall_2_1()); 
            match(input,RULE_INT,FOLLOW_RULE_INT_in_rule__IssueClass__Group_2__1__Impl5419); 
             after(grammarAccess.getIssueClassAccess().getINTTerminalRuleCall_2_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClass__Group_2__1__Impl"


    // $ANTLR start "rule__AnyValues__Group_2__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2677:1: rule__AnyValues__Group_2__0 : rule__AnyValues__Group_2__0__Impl rule__AnyValues__Group_2__1 ;
    public final void rule__AnyValues__Group_2__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2681:1: ( rule__AnyValues__Group_2__0__Impl rule__AnyValues__Group_2__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2682:2: rule__AnyValues__Group_2__0__Impl rule__AnyValues__Group_2__1
            {
            pushFollow(FOLLOW_rule__AnyValues__Group_2__0__Impl_in_rule__AnyValues__Group_2__05452);
            rule__AnyValues__Group_2__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__AnyValues__Group_2__1_in_rule__AnyValues__Group_2__05455);
            rule__AnyValues__Group_2__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_2__0"


    // $ANTLR start "rule__AnyValues__Group_2__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2689:1: rule__AnyValues__Group_2__0__Impl : ( RULE_INT ) ;
    public final void rule__AnyValues__Group_2__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2693:1: ( ( RULE_INT ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2694:1: ( RULE_INT )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2694:1: ( RULE_INT )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2695:1: RULE_INT
            {
             before(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_2_0()); 
            match(input,RULE_INT,FOLLOW_RULE_INT_in_rule__AnyValues__Group_2__0__Impl5482); 
             after(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_2__0__Impl"


    // $ANTLR start "rule__AnyValues__Group_2__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2706:1: rule__AnyValues__Group_2__1 : rule__AnyValues__Group_2__1__Impl ;
    public final void rule__AnyValues__Group_2__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2710:1: ( rule__AnyValues__Group_2__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2711:2: rule__AnyValues__Group_2__1__Impl
            {
            pushFollow(FOLLOW_rule__AnyValues__Group_2__1__Impl_in_rule__AnyValues__Group_2__15511);
            rule__AnyValues__Group_2__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_2__1"


    // $ANTLR start "rule__AnyValues__Group_2__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2717:1: rule__AnyValues__Group_2__1__Impl : ( RULE_ID ) ;
    public final void rule__AnyValues__Group_2__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2721:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2722:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2722:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2723:1: RULE_ID
            {
             before(grammarAccess.getAnyValuesAccess().getIDTerminalRuleCall_2_1()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__AnyValues__Group_2__1__Impl5538); 
             after(grammarAccess.getAnyValuesAccess().getIDTerminalRuleCall_2_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_2__1__Impl"


    // $ANTLR start "rule__AnyValues__Group_3__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2738:1: rule__AnyValues__Group_3__0 : rule__AnyValues__Group_3__0__Impl rule__AnyValues__Group_3__1 ;
    public final void rule__AnyValues__Group_3__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2742:1: ( rule__AnyValues__Group_3__0__Impl rule__AnyValues__Group_3__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2743:2: rule__AnyValues__Group_3__0__Impl rule__AnyValues__Group_3__1
            {
            pushFollow(FOLLOW_rule__AnyValues__Group_3__0__Impl_in_rule__AnyValues__Group_3__05571);
            rule__AnyValues__Group_3__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__AnyValues__Group_3__1_in_rule__AnyValues__Group_3__05574);
            rule__AnyValues__Group_3__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_3__0"


    // $ANTLR start "rule__AnyValues__Group_3__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2750:1: rule__AnyValues__Group_3__0__Impl : ( RULE_INT ) ;
    public final void rule__AnyValues__Group_3__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2754:1: ( ( RULE_INT ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2755:1: ( RULE_INT )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2755:1: ( RULE_INT )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2756:1: RULE_INT
            {
             before(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_3_0()); 
            match(input,RULE_INT,FOLLOW_RULE_INT_in_rule__AnyValues__Group_3__0__Impl5601); 
             after(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_3_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_3__0__Impl"


    // $ANTLR start "rule__AnyValues__Group_3__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2767:1: rule__AnyValues__Group_3__1 : rule__AnyValues__Group_3__1__Impl rule__AnyValues__Group_3__2 ;
    public final void rule__AnyValues__Group_3__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2771:1: ( rule__AnyValues__Group_3__1__Impl rule__AnyValues__Group_3__2 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2772:2: rule__AnyValues__Group_3__1__Impl rule__AnyValues__Group_3__2
            {
            pushFollow(FOLLOW_rule__AnyValues__Group_3__1__Impl_in_rule__AnyValues__Group_3__15630);
            rule__AnyValues__Group_3__1__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__AnyValues__Group_3__2_in_rule__AnyValues__Group_3__15633);
            rule__AnyValues__Group_3__2();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_3__1"


    // $ANTLR start "rule__AnyValues__Group_3__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2779:1: rule__AnyValues__Group_3__1__Impl : ( '.' ) ;
    public final void rule__AnyValues__Group_3__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2783:1: ( ( '.' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2784:1: ( '.' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2784:1: ( '.' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2785:1: '.'
            {
             before(grammarAccess.getAnyValuesAccess().getFullStopKeyword_3_1()); 
            match(input,16,FOLLOW_16_in_rule__AnyValues__Group_3__1__Impl5661); 
             after(grammarAccess.getAnyValuesAccess().getFullStopKeyword_3_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_3__1__Impl"


    // $ANTLR start "rule__AnyValues__Group_3__2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2798:1: rule__AnyValues__Group_3__2 : rule__AnyValues__Group_3__2__Impl ;
    public final void rule__AnyValues__Group_3__2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2802:1: ( rule__AnyValues__Group_3__2__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2803:2: rule__AnyValues__Group_3__2__Impl
            {
            pushFollow(FOLLOW_rule__AnyValues__Group_3__2__Impl_in_rule__AnyValues__Group_3__25692);
            rule__AnyValues__Group_3__2__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_3__2"


    // $ANTLR start "rule__AnyValues__Group_3__2__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2809:1: rule__AnyValues__Group_3__2__Impl : ( RULE_INT ) ;
    public final void rule__AnyValues__Group_3__2__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2813:1: ( ( RULE_INT ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2814:1: ( RULE_INT )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2814:1: ( RULE_INT )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2815:1: RULE_INT
            {
             before(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_3_2()); 
            match(input,RULE_INT,FOLLOW_RULE_INT_in_rule__AnyValues__Group_3__2__Impl5719); 
             after(grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_3_2()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__AnyValues__Group_3__2__Impl"


    // $ANTLR start "rule__SlashPath__Group__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2835:1: rule__SlashPath__Group__0 : rule__SlashPath__Group__0__Impl rule__SlashPath__Group__1 ;
    public final void rule__SlashPath__Group__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2839:1: ( rule__SlashPath__Group__0__Impl rule__SlashPath__Group__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2840:2: rule__SlashPath__Group__0__Impl rule__SlashPath__Group__1
            {
            pushFollow(FOLLOW_rule__SlashPath__Group__0__Impl_in_rule__SlashPath__Group__05757);
            rule__SlashPath__Group__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__SlashPath__Group__1_in_rule__SlashPath__Group__05760);
            rule__SlashPath__Group__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group__0"


    // $ANTLR start "rule__SlashPath__Group__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2847:1: rule__SlashPath__Group__0__Impl : ( RULE_ID ) ;
    public final void rule__SlashPath__Group__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2851:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2852:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2852:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2853:1: RULE_ID
            {
             before(grammarAccess.getSlashPathAccess().getIDTerminalRuleCall_0()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__SlashPath__Group__0__Impl5787); 
             after(grammarAccess.getSlashPathAccess().getIDTerminalRuleCall_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group__0__Impl"


    // $ANTLR start "rule__SlashPath__Group__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2864:1: rule__SlashPath__Group__1 : rule__SlashPath__Group__1__Impl ;
    public final void rule__SlashPath__Group__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2868:1: ( rule__SlashPath__Group__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2869:2: rule__SlashPath__Group__1__Impl
            {
            pushFollow(FOLLOW_rule__SlashPath__Group__1__Impl_in_rule__SlashPath__Group__15816);
            rule__SlashPath__Group__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group__1"


    // $ANTLR start "rule__SlashPath__Group__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2875:1: rule__SlashPath__Group__1__Impl : ( ( rule__SlashPath__Group_1__0 )* ) ;
    public final void rule__SlashPath__Group__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2879:1: ( ( ( rule__SlashPath__Group_1__0 )* ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2880:1: ( ( rule__SlashPath__Group_1__0 )* )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2880:1: ( ( rule__SlashPath__Group_1__0 )* )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2881:1: ( rule__SlashPath__Group_1__0 )*
            {
             before(grammarAccess.getSlashPathAccess().getGroup_1()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2882:1: ( rule__SlashPath__Group_1__0 )*
            loop19:
            do {
                int alt19=2;
                int LA19_0 = input.LA(1);

                if ( (LA19_0==13) ) {
                    alt19=1;
                }


                switch (alt19) {
            	case 1 :
            	    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2882:2: rule__SlashPath__Group_1__0
            	    {
            	    pushFollow(FOLLOW_rule__SlashPath__Group_1__0_in_rule__SlashPath__Group__1__Impl5843);
            	    rule__SlashPath__Group_1__0();

            	    state._fsp--;


            	    }
            	    break;

            	default :
            	    break loop19;
                }
            } while (true);

             after(grammarAccess.getSlashPathAccess().getGroup_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group__1__Impl"


    // $ANTLR start "rule__SlashPath__Group_1__0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2896:1: rule__SlashPath__Group_1__0 : rule__SlashPath__Group_1__0__Impl rule__SlashPath__Group_1__1 ;
    public final void rule__SlashPath__Group_1__0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2900:1: ( rule__SlashPath__Group_1__0__Impl rule__SlashPath__Group_1__1 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2901:2: rule__SlashPath__Group_1__0__Impl rule__SlashPath__Group_1__1
            {
            pushFollow(FOLLOW_rule__SlashPath__Group_1__0__Impl_in_rule__SlashPath__Group_1__05878);
            rule__SlashPath__Group_1__0__Impl();

            state._fsp--;

            pushFollow(FOLLOW_rule__SlashPath__Group_1__1_in_rule__SlashPath__Group_1__05881);
            rule__SlashPath__Group_1__1();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group_1__0"


    // $ANTLR start "rule__SlashPath__Group_1__0__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2908:1: rule__SlashPath__Group_1__0__Impl : ( '/' ) ;
    public final void rule__SlashPath__Group_1__0__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2912:1: ( ( '/' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2913:1: ( '/' )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2913:1: ( '/' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2914:1: '/'
            {
             before(grammarAccess.getSlashPathAccess().getSolidusKeyword_1_0()); 
            match(input,13,FOLLOW_13_in_rule__SlashPath__Group_1__0__Impl5909); 
             after(grammarAccess.getSlashPathAccess().getSolidusKeyword_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group_1__0__Impl"


    // $ANTLR start "rule__SlashPath__Group_1__1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2927:1: rule__SlashPath__Group_1__1 : rule__SlashPath__Group_1__1__Impl ;
    public final void rule__SlashPath__Group_1__1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2931:1: ( rule__SlashPath__Group_1__1__Impl )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2932:2: rule__SlashPath__Group_1__1__Impl
            {
            pushFollow(FOLLOW_rule__SlashPath__Group_1__1__Impl_in_rule__SlashPath__Group_1__15940);
            rule__SlashPath__Group_1__1__Impl();

            state._fsp--;


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group_1__1"


    // $ANTLR start "rule__SlashPath__Group_1__1__Impl"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2938:1: rule__SlashPath__Group_1__1__Impl : ( RULE_ID ) ;
    public final void rule__SlashPath__Group_1__1__Impl() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2942:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2943:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2943:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2944:1: RULE_ID
            {
             before(grammarAccess.getSlashPathAccess().getIDTerminalRuleCall_1_1()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__SlashPath__Group_1__1__Impl5967); 
             after(grammarAccess.getSlashPathAccess().getIDTerminalRuleCall_1_1()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__SlashPath__Group_1__1__Impl"


    // $ANTLR start "rule__ModelContainer__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2963:1: rule__ModelContainer__NameAssignment_0 : ( ruleAnalysisElement ) ;
    public final void rule__ModelContainer__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2967:1: ( ( ruleAnalysisElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2968:1: ( ruleAnalysisElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2968:1: ( ruleAnalysisElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2969:1: ruleAnalysisElement
            {
             before(grammarAccess.getModelContainerAccess().getNameAnalysisElementParserRuleCall_0_0()); 
            pushFollow(FOLLOW_ruleAnalysisElement_in_rule__ModelContainer__NameAssignment_06008);
            ruleAnalysisElement();

            state._fsp--;

             after(grammarAccess.getModelContainerAccess().getNameAnalysisElementParserRuleCall_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__NameAssignment_0"


    // $ANTLR start "rule__ModelContainer__ParameterAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2978:1: rule__ModelContainer__ParameterAssignment_1 : ( ruleParameterContainer ) ;
    public final void rule__ModelContainer__ParameterAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2982:1: ( ( ruleParameterContainer ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2983:1: ( ruleParameterContainer )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2983:1: ( ruleParameterContainer )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2984:1: ruleParameterContainer
            {
             before(grammarAccess.getModelContainerAccess().getParameterParameterContainerParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleParameterContainer_in_rule__ModelContainer__ParameterAssignment_16039);
            ruleParameterContainer();

            state._fsp--;

             after(grammarAccess.getModelContainerAccess().getParameterParameterContainerParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__ParameterAssignment_1"


    // $ANTLR start "rule__ModelContainer__IssuesAssignment_2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2993:1: rule__ModelContainer__IssuesAssignment_2 : ( ruleIssuesContainer ) ;
    public final void rule__ModelContainer__IssuesAssignment_2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2997:1: ( ( ruleIssuesContainer ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2998:1: ( ruleIssuesContainer )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2998:1: ( ruleIssuesContainer )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:2999:1: ruleIssuesContainer
            {
             before(grammarAccess.getModelContainerAccess().getIssuesIssuesContainerParserRuleCall_2_0()); 
            pushFollow(FOLLOW_ruleIssuesContainer_in_rule__ModelContainer__IssuesAssignment_26070);
            ruleIssuesContainer();

            state._fsp--;

             after(grammarAccess.getModelContainerAccess().getIssuesIssuesContainerParserRuleCall_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ModelContainer__IssuesAssignment_2"


    // $ANTLR start "rule__ParameterContainer__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3008:1: rule__ParameterContainer__NameAssignment_0 : ( ruleParametersElement ) ;
    public final void rule__ParameterContainer__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3012:1: ( ( ruleParametersElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3013:1: ( ruleParametersElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3013:1: ( ruleParametersElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3014:1: ruleParametersElement
            {
             before(grammarAccess.getParameterContainerAccess().getNameParametersElementParserRuleCall_0_0()); 
            pushFollow(FOLLOW_ruleParametersElement_in_rule__ParameterContainer__NameAssignment_06101);
            ruleParametersElement();

            state._fsp--;

             after(grammarAccess.getParameterContainerAccess().getNameParametersElementParserRuleCall_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterContainer__NameAssignment_0"


    // $ANTLR start "rule__ParameterContainer__ElementsAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3023:1: rule__ParameterContainer__ElementsAssignment_1 : ( ruleParameterElement ) ;
    public final void rule__ParameterContainer__ElementsAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3027:1: ( ( ruleParameterElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3028:1: ( ruleParameterElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3028:1: ( ruleParameterElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3029:1: ruleParameterElement
            {
             before(grammarAccess.getParameterContainerAccess().getElementsParameterElementParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleParameterElement_in_rule__ParameterContainer__ElementsAssignment_16132);
            ruleParameterElement();

            state._fsp--;

             after(grammarAccess.getParameterContainerAccess().getElementsParameterElementParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterContainer__ElementsAssignment_1"


    // $ANTLR start "rule__ParameterKeyValueElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3038:1: rule__ParameterKeyValueElement__NameAssignment_0 : ( RULE_ID ) ;
    public final void rule__ParameterKeyValueElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3042:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3043:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3043:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3044:1: RULE_ID
            {
             before(grammarAccess.getParameterKeyValueElementAccess().getNameIDTerminalRuleCall_0_0()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__ParameterKeyValueElement__NameAssignment_06163); 
             after(grammarAccess.getParameterKeyValueElementAccess().getNameIDTerminalRuleCall_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__NameAssignment_0"


    // $ANTLR start "rule__ParameterKeyValueElement__ValueAssignment_2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3053:1: rule__ParameterKeyValueElement__ValueAssignment_2 : ( ruleAnyValues ) ;
    public final void rule__ParameterKeyValueElement__ValueAssignment_2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3057:1: ( ( ruleAnyValues ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3058:1: ( ruleAnyValues )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3058:1: ( ruleAnyValues )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3059:1: ruleAnyValues
            {
             before(grammarAccess.getParameterKeyValueElementAccess().getValueAnyValuesParserRuleCall_2_0()); 
            pushFollow(FOLLOW_ruleAnyValues_in_rule__ParameterKeyValueElement__ValueAssignment_26194);
            ruleAnyValues();

            state._fsp--;

             after(grammarAccess.getParameterKeyValueElementAccess().getValueAnyValuesParserRuleCall_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyValueElement__ValueAssignment_2"


    // $ANTLR start "rule__ParameterKeyElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3068:1: rule__ParameterKeyElement__NameAssignment_0 : ( RULE_ID ) ;
    public final void rule__ParameterKeyElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3072:1: ( ( RULE_ID ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3073:1: ( RULE_ID )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3073:1: ( RULE_ID )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3074:1: RULE_ID
            {
             before(grammarAccess.getParameterKeyElementAccess().getNameIDTerminalRuleCall_0_0()); 
            match(input,RULE_ID,FOLLOW_RULE_ID_in_rule__ParameterKeyElement__NameAssignment_06225); 
             after(grammarAccess.getParameterKeyElementAccess().getNameIDTerminalRuleCall_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__ParameterKeyElement__NameAssignment_0"


    // $ANTLR start "rule__IssuesContainer__ElementsAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3083:1: rule__IssuesContainer__ElementsAssignment_1 : ( ruleIssueElement ) ;
    public final void rule__IssuesContainer__ElementsAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3087:1: ( ( ruleIssueElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3088:1: ( ruleIssueElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3088:1: ( ruleIssueElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3089:1: ruleIssueElement
            {
             before(grammarAccess.getIssuesContainerAccess().getElementsIssueElementParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleIssueElement_in_rule__IssuesContainer__ElementsAssignment_16256);
            ruleIssueElement();

            state._fsp--;

             after(grammarAccess.getIssuesContainerAccess().getElementsIssueElementParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuesContainer__ElementsAssignment_1"


    // $ANTLR start "rule__IssuesTitleElement__NameAssignment"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3098:1: rule__IssuesTitleElement__NameAssignment : ( ( 'Issues' ) ) ;
    public final void rule__IssuesTitleElement__NameAssignment() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3102:1: ( ( ( 'Issues' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3103:1: ( ( 'Issues' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3103:1: ( ( 'Issues' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3104:1: ( 'Issues' )
            {
             before(grammarAccess.getIssuesTitleElementAccess().getNameIssuesKeyword_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3105:1: ( 'Issues' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3106:1: 'Issues'
            {
             before(grammarAccess.getIssuesTitleElementAccess().getNameIssuesKeyword_0()); 
            match(input,36,FOLLOW_36_in_rule__IssuesTitleElement__NameAssignment6292); 
             after(grammarAccess.getIssuesTitleElementAccess().getNameIssuesKeyword_0()); 

            }

             after(grammarAccess.getIssuesTitleElementAccess().getNameIssuesKeyword_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuesTitleElement__NameAssignment"


    // $ANTLR start "rule__IssueElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3121:1: rule__IssueElement__NameAssignment_0 : ( ruleIssueTypes ) ;
    public final void rule__IssueElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3125:1: ( ( ruleIssueTypes ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3126:1: ( ruleIssueTypes )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3126:1: ( ruleIssueTypes )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3127:1: ruleIssueTypes
            {
             before(grammarAccess.getIssueElementAccess().getNameIssueTypesParserRuleCall_0_0()); 
            pushFollow(FOLLOW_ruleIssueTypes_in_rule__IssueElement__NameAssignment_06331);
            ruleIssueTypes();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getNameIssueTypesParserRuleCall_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__NameAssignment_0"


    // $ANTLR start "rule__IssueElement__NameAssignment_1_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3136:1: rule__IssueElement__NameAssignment_1_1 : ( ruleIssueTypes ) ;
    public final void rule__IssueElement__NameAssignment_1_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3140:1: ( ( ruleIssueTypes ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3141:1: ( ruleIssueTypes )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3141:1: ( ruleIssueTypes )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3142:1: ruleIssueTypes
            {
             before(grammarAccess.getIssueElementAccess().getNameIssueTypesParserRuleCall_1_1_0()); 
            pushFollow(FOLLOW_ruleIssueTypes_in_rule__IssueElement__NameAssignment_1_16362);
            ruleIssueTypes();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getNameIssueTypesParserRuleCall_1_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__NameAssignment_1_1"


    // $ANTLR start "rule__IssueElement__CommentAssignment_2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3151:1: rule__IssueElement__CommentAssignment_2 : ( ruleIssueSuppressComment ) ;
    public final void rule__IssueElement__CommentAssignment_2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3155:1: ( ( ruleIssueSuppressComment ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3156:1: ( ruleIssueSuppressComment )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3156:1: ( ruleIssueSuppressComment )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3157:1: ruleIssueSuppressComment
            {
             before(grammarAccess.getIssueElementAccess().getCommentIssueSuppressCommentParserRuleCall_2_0()); 
            pushFollow(FOLLOW_ruleIssueSuppressComment_in_rule__IssueElement__CommentAssignment_26393);
            ruleIssueSuppressComment();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getCommentIssueSuppressCommentParserRuleCall_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__CommentAssignment_2"


    // $ANTLR start "rule__IssueElement__MessageAssignment_4"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3166:1: rule__IssueElement__MessageAssignment_4 : ( RULE_STRING ) ;
    public final void rule__IssueElement__MessageAssignment_4() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3170:1: ( ( RULE_STRING ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3171:1: ( RULE_STRING )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3171:1: ( RULE_STRING )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3172:1: RULE_STRING
            {
             before(grammarAccess.getIssueElementAccess().getMessageSTRINGTerminalRuleCall_4_0()); 
            match(input,RULE_STRING,FOLLOW_RULE_STRING_in_rule__IssueElement__MessageAssignment_46424); 
             after(grammarAccess.getIssueElementAccess().getMessageSTRINGTerminalRuleCall_4_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__MessageAssignment_4"


    // $ANTLR start "rule__IssueElement__CategoriesAssignment_5"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3181:1: rule__IssueElement__CategoriesAssignment_5 : ( ruleIssueCategoryElement ) ;
    public final void rule__IssueElement__CategoriesAssignment_5() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3185:1: ( ( ruleIssueCategoryElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3186:1: ( ruleIssueCategoryElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3186:1: ( ruleIssueCategoryElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3187:1: ruleIssueCategoryElement
            {
             before(grammarAccess.getIssueElementAccess().getCategoriesIssueCategoryElementParserRuleCall_5_0()); 
            pushFollow(FOLLOW_ruleIssueCategoryElement_in_rule__IssueElement__CategoriesAssignment_56455);
            ruleIssueCategoryElement();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getCategoriesIssueCategoryElementParserRuleCall_5_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__CategoriesAssignment_5"


    // $ANTLR start "rule__IssueElement__KindsAssignment_6"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3196:1: rule__IssueElement__KindsAssignment_6 : ( ruleIssueKindElement ) ;
    public final void rule__IssueElement__KindsAssignment_6() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3200:1: ( ( ruleIssueKindElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3201:1: ( ruleIssueKindElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3201:1: ( ruleIssueKindElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3202:1: ruleIssueKindElement
            {
             before(grammarAccess.getIssueElementAccess().getKindsIssueKindElementParserRuleCall_6_0()); 
            pushFollow(FOLLOW_ruleIssueKindElement_in_rule__IssueElement__KindsAssignment_66486);
            ruleIssueKindElement();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getKindsIssueKindElementParserRuleCall_6_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__KindsAssignment_6"


    // $ANTLR start "rule__IssueElement__RelevanceAssignment_7"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3211:1: rule__IssueElement__RelevanceAssignment_7 : ( ruleIssueRelevanceElement ) ;
    public final void rule__IssueElement__RelevanceAssignment_7() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3215:1: ( ( ruleIssueRelevanceElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3216:1: ( ruleIssueRelevanceElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3216:1: ( ruleIssueRelevanceElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3217:1: ruleIssueRelevanceElement
            {
             before(grammarAccess.getIssueElementAccess().getRelevanceIssueRelevanceElementParserRuleCall_7_0()); 
            pushFollow(FOLLOW_ruleIssueRelevanceElement_in_rule__IssueElement__RelevanceAssignment_76517);
            ruleIssueRelevanceElement();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getRelevanceIssueRelevanceElementParserRuleCall_7_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__RelevanceAssignment_7"


    // $ANTLR start "rule__IssueElement__PackageAssignment_8"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3226:1: rule__IssueElement__PackageAssignment_8 : ( ruleIssuePackageElement ) ;
    public final void rule__IssueElement__PackageAssignment_8() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3230:1: ( ( ruleIssuePackageElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3231:1: ( ruleIssuePackageElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3231:1: ( ruleIssuePackageElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3232:1: ruleIssuePackageElement
            {
             before(grammarAccess.getIssueElementAccess().getPackageIssuePackageElementParserRuleCall_8_0()); 
            pushFollow(FOLLOW_ruleIssuePackageElement_in_rule__IssueElement__PackageAssignment_86548);
            ruleIssuePackageElement();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getPackageIssuePackageElementParserRuleCall_8_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__PackageAssignment_8"


    // $ANTLR start "rule__IssueElement__ClassAssignment_9"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3241:1: rule__IssueElement__ClassAssignment_9 : ( ruleIssueClassElement ) ;
    public final void rule__IssueElement__ClassAssignment_9() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3245:1: ( ( ruleIssueClassElement ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3246:1: ( ruleIssueClassElement )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3246:1: ( ruleIssueClassElement )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3247:1: ruleIssueClassElement
            {
             before(grammarAccess.getIssueElementAccess().getClassIssueClassElementParserRuleCall_9_0()); 
            pushFollow(FOLLOW_ruleIssueClassElement_in_rule__IssueElement__ClassAssignment_96579);
            ruleIssueClassElement();

            state._fsp--;

             after(grammarAccess.getIssueElementAccess().getClassIssueClassElementParserRuleCall_9_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueElement__ClassAssignment_9"


    // $ANTLR start "rule__IssueSuppressComment__ValueAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3256:1: rule__IssueSuppressComment__ValueAssignment_1 : ( ( rule__IssueSuppressComment__ValueAlternatives_1_0 ) ) ;
    public final void rule__IssueSuppressComment__ValueAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3260:1: ( ( ( rule__IssueSuppressComment__ValueAlternatives_1_0 ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3261:1: ( ( rule__IssueSuppressComment__ValueAlternatives_1_0 ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3261:1: ( ( rule__IssueSuppressComment__ValueAlternatives_1_0 ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3262:1: ( rule__IssueSuppressComment__ValueAlternatives_1_0 )
            {
             before(grammarAccess.getIssueSuppressCommentAccess().getValueAlternatives_1_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3263:1: ( rule__IssueSuppressComment__ValueAlternatives_1_0 )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3263:2: rule__IssueSuppressComment__ValueAlternatives_1_0
            {
            pushFollow(FOLLOW_rule__IssueSuppressComment__ValueAlternatives_1_0_in_rule__IssueSuppressComment__ValueAssignment_16610);
            rule__IssueSuppressComment__ValueAlternatives_1_0();

            state._fsp--;


            }

             after(grammarAccess.getIssueSuppressCommentAccess().getValueAlternatives_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueSuppressComment__ValueAssignment_1"


    // $ANTLR start "rule__IssueCategoryElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3272:1: rule__IssueCategoryElement__NameAssignment_0 : ( ( 'Categories:' ) ) ;
    public final void rule__IssueCategoryElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3276:1: ( ( ( 'Categories:' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3277:1: ( ( 'Categories:' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3277:1: ( ( 'Categories:' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3278:1: ( 'Categories:' )
            {
             before(grammarAccess.getIssueCategoryElementAccess().getNameCategoriesKeyword_0_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3279:1: ( 'Categories:' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3280:1: 'Categories:'
            {
             before(grammarAccess.getIssueCategoryElementAccess().getNameCategoriesKeyword_0_0()); 
            match(input,37,FOLLOW_37_in_rule__IssueCategoryElement__NameAssignment_06648); 
             after(grammarAccess.getIssueCategoryElementAccess().getNameCategoriesKeyword_0_0()); 

            }

             after(grammarAccess.getIssueCategoryElementAccess().getNameCategoriesKeyword_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__NameAssignment_0"


    // $ANTLR start "rule__IssueCategoryElement__ElementsAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3295:1: rule__IssueCategoryElement__ElementsAssignment_1 : ( ruleIssueCategories ) ;
    public final void rule__IssueCategoryElement__ElementsAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3299:1: ( ( ruleIssueCategories ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3300:1: ( ruleIssueCategories )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3300:1: ( ruleIssueCategories )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3301:1: ruleIssueCategories
            {
             before(grammarAccess.getIssueCategoryElementAccess().getElementsIssueCategoriesParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleIssueCategories_in_rule__IssueCategoryElement__ElementsAssignment_16687);
            ruleIssueCategories();

            state._fsp--;

             after(grammarAccess.getIssueCategoryElementAccess().getElementsIssueCategoriesParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__ElementsAssignment_1"


    // $ANTLR start "rule__IssueCategoryElement__ElementsAssignment_2_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3310:1: rule__IssueCategoryElement__ElementsAssignment_2_1 : ( ruleIssueCategories ) ;
    public final void rule__IssueCategoryElement__ElementsAssignment_2_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3314:1: ( ( ruleIssueCategories ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3315:1: ( ruleIssueCategories )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3315:1: ( ruleIssueCategories )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3316:1: ruleIssueCategories
            {
             before(grammarAccess.getIssueCategoryElementAccess().getElementsIssueCategoriesParserRuleCall_2_1_0()); 
            pushFollow(FOLLOW_ruleIssueCategories_in_rule__IssueCategoryElement__ElementsAssignment_2_16718);
            ruleIssueCategories();

            state._fsp--;

             after(grammarAccess.getIssueCategoryElementAccess().getElementsIssueCategoriesParserRuleCall_2_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategoryElement__ElementsAssignment_2_1"


    // $ANTLR start "rule__IssueKindElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3325:1: rule__IssueKindElement__NameAssignment_0 : ( ( 'Kinds:' ) ) ;
    public final void rule__IssueKindElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3329:1: ( ( ( 'Kinds:' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3330:1: ( ( 'Kinds:' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3330:1: ( ( 'Kinds:' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3331:1: ( 'Kinds:' )
            {
             before(grammarAccess.getIssueKindElementAccess().getNameKindsKeyword_0_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3332:1: ( 'Kinds:' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3333:1: 'Kinds:'
            {
             before(grammarAccess.getIssueKindElementAccess().getNameKindsKeyword_0_0()); 
            match(input,38,FOLLOW_38_in_rule__IssueKindElement__NameAssignment_06754); 
             after(grammarAccess.getIssueKindElementAccess().getNameKindsKeyword_0_0()); 

            }

             after(grammarAccess.getIssueKindElementAccess().getNameKindsKeyword_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__NameAssignment_0"


    // $ANTLR start "rule__IssueKindElement__ElementsAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3348:1: rule__IssueKindElement__ElementsAssignment_1 : ( ruleIssueKinds ) ;
    public final void rule__IssueKindElement__ElementsAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3352:1: ( ( ruleIssueKinds ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3353:1: ( ruleIssueKinds )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3353:1: ( ruleIssueKinds )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3354:1: ruleIssueKinds
            {
             before(grammarAccess.getIssueKindElementAccess().getElementsIssueKindsParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleIssueKinds_in_rule__IssueKindElement__ElementsAssignment_16793);
            ruleIssueKinds();

            state._fsp--;

             after(grammarAccess.getIssueKindElementAccess().getElementsIssueKindsParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__ElementsAssignment_1"


    // $ANTLR start "rule__IssueKindElement__ElementsAssignment_2_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3363:1: rule__IssueKindElement__ElementsAssignment_2_1 : ( ruleIssueKinds ) ;
    public final void rule__IssueKindElement__ElementsAssignment_2_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3367:1: ( ( ruleIssueKinds ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3368:1: ( ruleIssueKinds )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3368:1: ( ruleIssueKinds )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3369:1: ruleIssueKinds
            {
             before(grammarAccess.getIssueKindElementAccess().getElementsIssueKindsParserRuleCall_2_1_0()); 
            pushFollow(FOLLOW_ruleIssueKinds_in_rule__IssueKindElement__ElementsAssignment_2_16824);
            ruleIssueKinds();

            state._fsp--;

             after(grammarAccess.getIssueKindElementAccess().getElementsIssueKindsParserRuleCall_2_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueKindElement__ElementsAssignment_2_1"


    // $ANTLR start "rule__IssueRelevanceElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3378:1: rule__IssueRelevanceElement__NameAssignment_0 : ( ( 'Relevance:' ) ) ;
    public final void rule__IssueRelevanceElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3382:1: ( ( ( 'Relevance:' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3383:1: ( ( 'Relevance:' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3383:1: ( ( 'Relevance:' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3384:1: ( 'Relevance:' )
            {
             before(grammarAccess.getIssueRelevanceElementAccess().getNameRelevanceKeyword_0_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3385:1: ( 'Relevance:' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3386:1: 'Relevance:'
            {
             before(grammarAccess.getIssueRelevanceElementAccess().getNameRelevanceKeyword_0_0()); 
            match(input,39,FOLLOW_39_in_rule__IssueRelevanceElement__NameAssignment_06860); 
             after(grammarAccess.getIssueRelevanceElementAccess().getNameRelevanceKeyword_0_0()); 

            }

             after(grammarAccess.getIssueRelevanceElementAccess().getNameRelevanceKeyword_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueRelevanceElement__NameAssignment_0"


    // $ANTLR start "rule__IssueRelevanceElement__RelevanceAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3401:1: rule__IssueRelevanceElement__RelevanceAssignment_1 : ( RULE_INT ) ;
    public final void rule__IssueRelevanceElement__RelevanceAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3405:1: ( ( RULE_INT ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3406:1: ( RULE_INT )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3406:1: ( RULE_INT )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3407:1: RULE_INT
            {
             before(grammarAccess.getIssueRelevanceElementAccess().getRelevanceINTTerminalRuleCall_1_0()); 
            match(input,RULE_INT,FOLLOW_RULE_INT_in_rule__IssueRelevanceElement__RelevanceAssignment_16899); 
             after(grammarAccess.getIssueRelevanceElementAccess().getRelevanceINTTerminalRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueRelevanceElement__RelevanceAssignment_1"


    // $ANTLR start "rule__IssuePackageElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3416:1: rule__IssuePackageElement__NameAssignment_0 : ( ( 'Package:' ) ) ;
    public final void rule__IssuePackageElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3420:1: ( ( ( 'Package:' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3421:1: ( ( 'Package:' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3421:1: ( ( 'Package:' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3422:1: ( 'Package:' )
            {
             before(grammarAccess.getIssuePackageElementAccess().getNamePackageKeyword_0_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3423:1: ( 'Package:' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3424:1: 'Package:'
            {
             before(grammarAccess.getIssuePackageElementAccess().getNamePackageKeyword_0_0()); 
            match(input,40,FOLLOW_40_in_rule__IssuePackageElement__NameAssignment_06935); 
             after(grammarAccess.getIssuePackageElementAccess().getNamePackageKeyword_0_0()); 

            }

             after(grammarAccess.getIssuePackageElementAccess().getNamePackageKeyword_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuePackageElement__NameAssignment_0"


    // $ANTLR start "rule__IssuePackageElement__PackageAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3439:1: rule__IssuePackageElement__PackageAssignment_1 : ( ruleSlashPath ) ;
    public final void rule__IssuePackageElement__PackageAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3443:1: ( ( ruleSlashPath ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3444:1: ( ruleSlashPath )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3444:1: ( ruleSlashPath )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3445:1: ruleSlashPath
            {
             before(grammarAccess.getIssuePackageElementAccess().getPackageSlashPathParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleSlashPath_in_rule__IssuePackageElement__PackageAssignment_16974);
            ruleSlashPath();

            state._fsp--;

             after(grammarAccess.getIssuePackageElementAccess().getPackageSlashPathParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssuePackageElement__PackageAssignment_1"


    // $ANTLR start "rule__IssueClassElement__NameAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3454:1: rule__IssueClassElement__NameAssignment_0 : ( ( 'Class:' ) ) ;
    public final void rule__IssueClassElement__NameAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3458:1: ( ( ( 'Class:' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3459:1: ( ( 'Class:' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3459:1: ( ( 'Class:' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3460:1: ( 'Class:' )
            {
             before(grammarAccess.getIssueClassElementAccess().getNameClassKeyword_0_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3461:1: ( 'Class:' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3462:1: 'Class:'
            {
             before(grammarAccess.getIssueClassElementAccess().getNameClassKeyword_0_0()); 
            match(input,41,FOLLOW_41_in_rule__IssueClassElement__NameAssignment_07010); 
             after(grammarAccess.getIssueClassElementAccess().getNameClassKeyword_0_0()); 

            }

             after(grammarAccess.getIssueClassElementAccess().getNameClassKeyword_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClassElement__NameAssignment_0"


    // $ANTLR start "rule__IssueClassElement__ClassAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3477:1: rule__IssueClassElement__ClassAssignment_1 : ( ruleIssueClass ) ;
    public final void rule__IssueClassElement__ClassAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3481:1: ( ( ruleIssueClass ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3482:1: ( ruleIssueClass )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3482:1: ( ruleIssueClass )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3483:1: ruleIssueClass
            {
             before(grammarAccess.getIssueClassElementAccess().getClassIssueClassParserRuleCall_1_0()); 
            pushFollow(FOLLOW_ruleIssueClass_in_rule__IssueClassElement__ClassAssignment_17049);
            ruleIssueClass();

            state._fsp--;

             after(grammarAccess.getIssueClassElementAccess().getClassIssueClassParserRuleCall_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueClassElement__ClassAssignment_1"


    // $ANTLR start "rule__IssueCategories__BugAssignment_0"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3492:1: rule__IssueCategories__BugAssignment_0 : ( ( 'bug' ) ) ;
    public final void rule__IssueCategories__BugAssignment_0() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3496:1: ( ( ( 'bug' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3497:1: ( ( 'bug' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3497:1: ( ( 'bug' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3498:1: ( 'bug' )
            {
             before(grammarAccess.getIssueCategoriesAccess().getBugBugKeyword_0_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3499:1: ( 'bug' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3500:1: 'bug'
            {
             before(grammarAccess.getIssueCategoriesAccess().getBugBugKeyword_0_0()); 
            match(input,42,FOLLOW_42_in_rule__IssueCategories__BugAssignment_07085); 
             after(grammarAccess.getIssueCategoriesAccess().getBugBugKeyword_0_0()); 

            }

             after(grammarAccess.getIssueCategoriesAccess().getBugBugKeyword_0_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategories__BugAssignment_0"


    // $ANTLR start "rule__IssueCategories__SmellAssignment_1"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3515:1: rule__IssueCategories__SmellAssignment_1 : ( ( 'smell' ) ) ;
    public final void rule__IssueCategories__SmellAssignment_1() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3519:1: ( ( ( 'smell' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3520:1: ( ( 'smell' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3520:1: ( ( 'smell' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3521:1: ( 'smell' )
            {
             before(grammarAccess.getIssueCategoriesAccess().getSmellSmellKeyword_1_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3522:1: ( 'smell' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3523:1: 'smell'
            {
             before(grammarAccess.getIssueCategoriesAccess().getSmellSmellKeyword_1_0()); 
            match(input,43,FOLLOW_43_in_rule__IssueCategories__SmellAssignment_17129); 
             after(grammarAccess.getIssueCategoriesAccess().getSmellSmellKeyword_1_0()); 

            }

             after(grammarAccess.getIssueCategoriesAccess().getSmellSmellKeyword_1_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategories__SmellAssignment_1"


    // $ANTLR start "rule__IssueCategories__PerformanceAssignment_2"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3538:1: rule__IssueCategories__PerformanceAssignment_2 : ( ( 'performance' ) ) ;
    public final void rule__IssueCategories__PerformanceAssignment_2() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3542:1: ( ( ( 'performance' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3543:1: ( ( 'performance' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3543:1: ( ( 'performance' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3544:1: ( 'performance' )
            {
             before(grammarAccess.getIssueCategoriesAccess().getPerformancePerformanceKeyword_2_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3545:1: ( 'performance' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3546:1: 'performance'
            {
             before(grammarAccess.getIssueCategoriesAccess().getPerformancePerformanceKeyword_2_0()); 
            match(input,44,FOLLOW_44_in_rule__IssueCategories__PerformanceAssignment_27173); 
             after(grammarAccess.getIssueCategoriesAccess().getPerformancePerformanceKeyword_2_0()); 

            }

             after(grammarAccess.getIssueCategoriesAccess().getPerformancePerformanceKeyword_2_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategories__PerformanceAssignment_2"


    // $ANTLR start "rule__IssueCategories__ComprehensibilityAssignment_3"
    // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3561:1: rule__IssueCategories__ComprehensibilityAssignment_3 : ( ( 'comprehensibility' ) ) ;
    public final void rule__IssueCategories__ComprehensibilityAssignment_3() throws RecognitionException {

        		int stackSize = keepStackSize();
            
        try {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3565:1: ( ( ( 'comprehensibility' ) ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3566:1: ( ( 'comprehensibility' ) )
            {
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3566:1: ( ( 'comprehensibility' ) )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3567:1: ( 'comprehensibility' )
            {
             before(grammarAccess.getIssueCategoriesAccess().getComprehensibilityComprehensibilityKeyword_3_0()); 
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3568:1: ( 'comprehensibility' )
            // ../org.opalj.bdl.ui/src-gen/org/opalj/bdl/ui/contentassist/antlr/internal/InternalBDL.g:3569:1: 'comprehensibility'
            {
             before(grammarAccess.getIssueCategoriesAccess().getComprehensibilityComprehensibilityKeyword_3_0()); 
            match(input,45,FOLLOW_45_in_rule__IssueCategories__ComprehensibilityAssignment_37217); 
             after(grammarAccess.getIssueCategoriesAccess().getComprehensibilityComprehensibilityKeyword_3_0()); 

            }

             after(grammarAccess.getIssueCategoriesAccess().getComprehensibilityComprehensibilityKeyword_3_0()); 

            }


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {

            	restoreStackSize(stackSize);

        }
        return ;
    }
    // $ANTLR end "rule__IssueCategories__ComprehensibilityAssignment_3"

    // Delegated rules


 

    public static final BitSet FOLLOW_ruleModel_in_entryRuleModel61 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleModel68 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleModelContainer_in_ruleModel94 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleModelContainer_in_entryRuleModelContainer120 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleModelContainer127 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__Group__0_in_ruleModelContainer153 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleAnalysisElement_in_entryRuleAnalysisElement180 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleAnalysisElement187 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnalysisElement__Group__0_in_ruleAnalysisElement213 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterContainer_in_entryRuleParameterContainer240 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterContainer247 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterContainer__Group__0_in_ruleParameterContainer273 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParametersElement_in_entryRuleParametersElement300 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParametersElement307 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_11_in_ruleParametersElement334 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterElement_in_entryRuleParameterElement362 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterElement369 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterElement__Alternatives_in_ruleParameterElement395 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyValueElement_in_entryRuleParameterKeyValueElement422 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterKeyValueElement429 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Group__0_in_ruleParameterKeyValueElement455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyElement_in_entryRuleParameterKeyElement482 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterKeyElement489 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyElement__Group__0_in_ruleParameterKeyElement515 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuesContainer_in_entryRuleIssuesContainer542 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssuesContainer549 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuesContainer__Group__0_in_ruleIssuesContainer575 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuesTitleElement_in_entryRuleIssuesTitleElement602 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssuesTitleElement609 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuesTitleElement__NameAssignment_in_ruleIssuesTitleElement635 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueElement_in_entryRuleIssueElement662 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueElement669 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__0_in_ruleIssueElement695 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueSuppressComment_in_entryRuleIssueSuppressComment722 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueSuppressComment729 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__Group__0_in_ruleIssueSuppressComment755 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategoryElement_in_entryRuleIssueCategoryElement782 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueCategoryElement789 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group__0_in_ruleIssueCategoryElement815 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueKindElement_in_entryRuleIssueKindElement842 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueKindElement849 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group__0_in_ruleIssueKindElement875 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueRelevanceElement_in_entryRuleIssueRelevanceElement902 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueRelevanceElement909 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueRelevanceElement__Group__0_in_ruleIssueRelevanceElement935 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuePackageElement_in_entryRuleIssuePackageElement962 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssuePackageElement969 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuePackageElement__Group__0_in_ruleIssuePackageElement995 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueClassElement_in_entryRuleIssueClassElement1022 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueClassElement1029 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClassElement__Group__0_in_ruleIssueClassElement1055 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueTypes_in_entryRuleIssueTypes1082 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueTypes1089 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueTypes__Alternatives_in_ruleIssueTypes1115 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategories_in_entryRuleIssueCategories1142 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueCategories1149 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategories__Alternatives_in_ruleIssueCategories1175 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueKinds_in_entryRuleIssueKinds1202 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueKinds1209 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKinds__Alternatives_in_ruleIssueKinds1235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueClass_in_entryRuleIssueClass1262 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueClass1269 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group__0_in_ruleIssueClass1295 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleAnyValues_in_entryRuleAnyValues1322 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleAnyValues1329 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Alternatives_in_ruleAnyValues1355 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleSlashPath_in_entryRuleSlashPath1386 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleSlashPath1393 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__SlashPath__Group__0_in_ruleSlashPath1419 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__AnalysisElement__Alternatives_11457 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_12_in_rule__AnalysisElement__Alternatives_11475 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_13_in_rule__AnalysisElement__Alternatives_11495 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_14_in_rule__AnalysisElement__Alternatives_11515 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_rule__AnalysisElement__Alternatives_11535 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_16_in_rule__AnalysisElement__Alternatives_11555 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyValueElement_in_rule__ParameterElement__Alternatives1589 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyElement_in_rule__ParameterElement__Alternatives1606 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_12_in_rule__ParameterKeyValueElement__Alternatives_11639 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_rule__ParameterKeyValueElement__Alternatives_11659 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__IssueSuppressComment__ValueAlternatives_1_01693 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_STRING_in_rule__IssueSuppressComment__ValueAlternatives_1_01710 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_18_in_rule__IssueTypes__Alternatives1743 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_19_in_rule__IssueTypes__Alternatives1763 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_20_in_rule__IssueTypes__Alternatives1783 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_21_in_rule__IssueTypes__Alternatives1803 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_rule__IssueTypes__Alternatives1823 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategories__BugAssignment_0_in_rule__IssueCategories__Alternatives1857 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategories__SmellAssignment_1_in_rule__IssueCategories__Alternatives1875 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategories__PerformanceAssignment_2_in_rule__IssueCategories__Alternatives1893 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategories__ComprehensibilityAssignment_3_in_rule__IssueCategories__Alternatives1911 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_rule__IssueKinds__Alternatives1945 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_24_in_rule__IssueKinds__Alternatives1965 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_rule__IssueKinds__Alternatives1985 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_rule__IssueKinds__Alternatives2005 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_rule__IssueKinds__Alternatives2025 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_rule__IssueKinds__Alternatives2045 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_rule__AnyValues__Alternatives2079 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__AnyValues__Alternatives2096 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_2__0_in_rule__AnyValues__Alternatives2113 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_3__0_in_rule__AnyValues__Alternatives2131 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__Group__0__Impl_in_rule__ModelContainer__Group__02163 = new BitSet(new long[]{0x0000000000000800L});
    public static final BitSet FOLLOW_rule__ModelContainer__Group__1_in_rule__ModelContainer__Group__02166 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__NameAssignment_0_in_rule__ModelContainer__Group__0__Impl2193 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__Group__1__Impl_in_rule__ModelContainer__Group__12223 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_rule__ModelContainer__Group__2_in_rule__ModelContainer__Group__12226 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__ParameterAssignment_1_in_rule__ModelContainer__Group__1__Impl2253 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__Group__2__Impl_in_rule__ModelContainer__Group__22283 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ModelContainer__IssuesAssignment_2_in_rule__ModelContainer__Group__2__Impl2310 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnalysisElement__Group__0__Impl_in_rule__AnalysisElement__Group__02346 = new BitSet(new long[]{0x000000000001F010L});
    public static final BitSet FOLLOW_rule__AnalysisElement__Group__1_in_rule__AnalysisElement__Group__02349 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_rule__AnalysisElement__Group__0__Impl2377 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnalysisElement__Group__1__Impl_in_rule__AnalysisElement__Group__12408 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnalysisElement__Alternatives_1_in_rule__AnalysisElement__Group__1__Impl2437 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_rule__AnalysisElement__Alternatives_1_in_rule__AnalysisElement__Group__1__Impl2449 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_rule__ParameterContainer__Group__0__Impl_in_rule__ParameterContainer__Group__02486 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_rule__ParameterContainer__Group__1_in_rule__ParameterContainer__Group__02489 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterContainer__NameAssignment_0_in_rule__ParameterContainer__Group__0__Impl2516 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterContainer__Group__1__Impl_in_rule__ParameterContainer__Group__12546 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterContainer__ElementsAssignment_1_in_rule__ParameterContainer__Group__1__Impl2575 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_rule__ParameterContainer__ElementsAssignment_1_in_rule__ParameterContainer__Group__1__Impl2587 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Group__0__Impl_in_rule__ParameterKeyValueElement__Group__02624 = new BitSet(new long[]{0x0000000000021000L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Group__1_in_rule__ParameterKeyValueElement__Group__02627 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__NameAssignment_0_in_rule__ParameterKeyValueElement__Group__0__Impl2654 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Group__1__Impl_in_rule__ParameterKeyValueElement__Group__12684 = new BitSet(new long[]{0x0000000000000050L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Group__2_in_rule__ParameterKeyValueElement__Group__12687 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Alternatives_1_in_rule__ParameterKeyValueElement__Group__1__Impl2714 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__Group__2__Impl_in_rule__ParameterKeyValueElement__Group__22744 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyValueElement__ValueAssignment_2_in_rule__ParameterKeyValueElement__Group__2__Impl2771 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyElement__Group__0__Impl_in_rule__ParameterKeyElement__Group__02807 = new BitSet(new long[]{0x0000000040000000L});
    public static final BitSet FOLLOW_rule__ParameterKeyElement__Group__1_in_rule__ParameterKeyElement__Group__02810 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyElement__NameAssignment_0_in_rule__ParameterKeyElement__Group__0__Impl2837 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__ParameterKeyElement__Group__1__Impl_in_rule__ParameterKeyElement__Group__12867 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_rule__ParameterKeyElement__Group__1__Impl2895 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuesContainer__Group__0__Impl_in_rule__IssuesContainer__Group__02930 = new BitSet(new long[]{0x00000000007C0000L});
    public static final BitSet FOLLOW_rule__IssuesContainer__Group__1_in_rule__IssuesContainer__Group__02933 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuesTitleElement_in_rule__IssuesContainer__Group__0__Impl2960 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuesContainer__Group__1__Impl_in_rule__IssuesContainer__Group__12989 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuesContainer__ElementsAssignment_1_in_rule__IssuesContainer__Group__1__Impl3016 = new BitSet(new long[]{0x00000000007C0002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__0__Impl_in_rule__IssueElement__Group__03051 = new BitSet(new long[]{0x0000000280008000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__1_in_rule__IssueElement__Group__03054 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__NameAssignment_0_in_rule__IssueElement__Group__0__Impl3081 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__1__Impl_in_rule__IssueElement__Group__13111 = new BitSet(new long[]{0x0000000280008000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__2_in_rule__IssueElement__Group__13114 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group_1__0_in_rule__IssueElement__Group__1__Impl3141 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__2__Impl_in_rule__IssueElement__Group__23172 = new BitSet(new long[]{0x0000000280008000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__3_in_rule__IssueElement__Group__23175 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__CommentAssignment_2_in_rule__IssueElement__Group__2__Impl3202 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__3__Impl_in_rule__IssueElement__Group__33233 = new BitSet(new long[]{0x0000002000000020L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__4_in_rule__IssueElement__Group__33236 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_rule__IssueElement__Group__3__Impl3264 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__4__Impl_in_rule__IssueElement__Group__43295 = new BitSet(new long[]{0x0000002000000020L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__5_in_rule__IssueElement__Group__43298 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__MessageAssignment_4_in_rule__IssueElement__Group__4__Impl3325 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__5__Impl_in_rule__IssueElement__Group__53356 = new BitSet(new long[]{0x0000004000000000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__6_in_rule__IssueElement__Group__53359 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__CategoriesAssignment_5_in_rule__IssueElement__Group__5__Impl3386 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__6__Impl_in_rule__IssueElement__Group__63416 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__7_in_rule__IssueElement__Group__63419 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__KindsAssignment_6_in_rule__IssueElement__Group__6__Impl3446 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__7__Impl_in_rule__IssueElement__Group__73476 = new BitSet(new long[]{0x0000010000000000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__8_in_rule__IssueElement__Group__73479 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__RelevanceAssignment_7_in_rule__IssueElement__Group__7__Impl3506 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__8__Impl_in_rule__IssueElement__Group__83536 = new BitSet(new long[]{0x0000020000000000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__9_in_rule__IssueElement__Group__83539 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__PackageAssignment_8_in_rule__IssueElement__Group__8__Impl3566 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__9__Impl_in_rule__IssueElement__Group__93596 = new BitSet(new long[]{0x0000000100000000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__10_in_rule__IssueElement__Group__93599 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__ClassAssignment_9_in_rule__IssueElement__Group__9__Impl3626 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group__10__Impl_in_rule__IssueElement__Group__103656 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_rule__IssueElement__Group__10__Impl3684 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group_1__0__Impl_in_rule__IssueElement__Group_1__03737 = new BitSet(new long[]{0x00000000007C0000L});
    public static final BitSet FOLLOW_rule__IssueElement__Group_1__1_in_rule__IssueElement__Group_1__03740 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_rule__IssueElement__Group_1__0__Impl3768 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__Group_1__1__Impl_in_rule__IssueElement__Group_1__13799 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueElement__NameAssignment_1_1_in_rule__IssueElement__Group_1__1__Impl3826 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__Group__0__Impl_in_rule__IssueSuppressComment__Group__03860 = new BitSet(new long[]{0x0000000000000030L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__Group__1_in_rule__IssueSuppressComment__Group__03863 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_rule__IssueSuppressComment__Group__0__Impl3891 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__Group__1__Impl_in_rule__IssueSuppressComment__Group__13922 = new BitSet(new long[]{0x0000000400000000L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__Group__2_in_rule__IssueSuppressComment__Group__13925 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__ValueAssignment_1_in_rule__IssueSuppressComment__Group__1__Impl3952 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__Group__2__Impl_in_rule__IssueSuppressComment__Group__23982 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_rule__IssueSuppressComment__Group__2__Impl4010 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group__0__Impl_in_rule__IssueCategoryElement__Group__04047 = new BitSet(new long[]{0x00003C0000000000L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group__1_in_rule__IssueCategoryElement__Group__04050 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__NameAssignment_0_in_rule__IssueCategoryElement__Group__0__Impl4077 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group__1__Impl_in_rule__IssueCategoryElement__Group__14107 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group__2_in_rule__IssueCategoryElement__Group__14110 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__ElementsAssignment_1_in_rule__IssueCategoryElement__Group__1__Impl4137 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group__2__Impl_in_rule__IssueCategoryElement__Group__24167 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group_2__0_in_rule__IssueCategoryElement__Group__2__Impl4194 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group_2__0__Impl_in_rule__IssueCategoryElement__Group_2__04231 = new BitSet(new long[]{0x00003C0000000000L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group_2__1_in_rule__IssueCategoryElement__Group_2__04234 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_rule__IssueCategoryElement__Group_2__0__Impl4262 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__Group_2__1__Impl_in_rule__IssueCategoryElement__Group_2__14293 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueCategoryElement__ElementsAssignment_2_1_in_rule__IssueCategoryElement__Group_2__1__Impl4320 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group__0__Impl_in_rule__IssueKindElement__Group__04354 = new BitSet(new long[]{0x000000001F800000L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group__1_in_rule__IssueKindElement__Group__04357 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__NameAssignment_0_in_rule__IssueKindElement__Group__0__Impl4384 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group__1__Impl_in_rule__IssueKindElement__Group__14414 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group__2_in_rule__IssueKindElement__Group__14417 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__ElementsAssignment_1_in_rule__IssueKindElement__Group__1__Impl4444 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group__2__Impl_in_rule__IssueKindElement__Group__24474 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group_2__0_in_rule__IssueKindElement__Group__2__Impl4501 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group_2__0__Impl_in_rule__IssueKindElement__Group_2__04538 = new BitSet(new long[]{0x000000001F800000L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group_2__1_in_rule__IssueKindElement__Group_2__04541 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_15_in_rule__IssueKindElement__Group_2__0__Impl4569 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__Group_2__1__Impl_in_rule__IssueKindElement__Group_2__14600 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueKindElement__ElementsAssignment_2_1_in_rule__IssueKindElement__Group_2__1__Impl4627 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueRelevanceElement__Group__0__Impl_in_rule__IssueRelevanceElement__Group__04661 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_rule__IssueRelevanceElement__Group__1_in_rule__IssueRelevanceElement__Group__04664 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueRelevanceElement__NameAssignment_0_in_rule__IssueRelevanceElement__Group__0__Impl4691 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueRelevanceElement__Group__1__Impl_in_rule__IssueRelevanceElement__Group__14721 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueRelevanceElement__RelevanceAssignment_1_in_rule__IssueRelevanceElement__Group__1__Impl4748 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuePackageElement__Group__0__Impl_in_rule__IssuePackageElement__Group__04782 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_rule__IssuePackageElement__Group__1_in_rule__IssuePackageElement__Group__04785 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuePackageElement__NameAssignment_0_in_rule__IssuePackageElement__Group__0__Impl4812 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuePackageElement__Group__1__Impl_in_rule__IssuePackageElement__Group__14842 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssuePackageElement__PackageAssignment_1_in_rule__IssuePackageElement__Group__1__Impl4869 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClassElement__Group__0__Impl_in_rule__IssueClassElement__Group__04903 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_rule__IssueClassElement__Group__1_in_rule__IssueClassElement__Group__04906 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClassElement__NameAssignment_0_in_rule__IssueClassElement__Group__0__Impl4933 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClassElement__Group__1__Impl_in_rule__IssueClassElement__Group__14963 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClassElement__ClassAssignment_1_in_rule__IssueClassElement__Group__1__Impl4990 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group__0__Impl_in_rule__IssueClass__Group__05024 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_rule__IssueClass__Group__1_in_rule__IssueClass__Group__05027 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__IssueClass__Group__0__Impl5054 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group__1__Impl_in_rule__IssueClass__Group__15083 = new BitSet(new long[]{0x0000000800000000L});
    public static final BitSet FOLLOW_rule__IssueClass__Group__2_in_rule__IssueClass__Group__15086 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_1__0_in_rule__IssueClass__Group__1__Impl5113 = new BitSet(new long[]{0x0000000800000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group__2__Impl_in_rule__IssueClass__Group__25144 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_2__0_in_rule__IssueClass__Group__2__Impl5171 = new BitSet(new long[]{0x0000000800000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_1__0__Impl_in_rule__IssueClass__Group_1__05208 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_1__1_in_rule__IssueClass__Group_1__05211 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_rule__IssueClass__Group_1__0__Impl5239 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_1__1__Impl_in_rule__IssueClass__Group_1__15270 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__IssueClass__Group_1__1__Impl5297 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_2__0__Impl_in_rule__IssueClass__Group_2__05330 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_2__1_in_rule__IssueClass__Group_2__05333 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_rule__IssueClass__Group_2__0__Impl5361 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueClass__Group_2__1__Impl_in_rule__IssueClass__Group_2__15392 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_rule__IssueClass__Group_2__1__Impl5419 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_2__0__Impl_in_rule__AnyValues__Group_2__05452 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_2__1_in_rule__AnyValues__Group_2__05455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_rule__AnyValues__Group_2__0__Impl5482 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_2__1__Impl_in_rule__AnyValues__Group_2__15511 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__AnyValues__Group_2__1__Impl5538 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_3__0__Impl_in_rule__AnyValues__Group_3__05571 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_3__1_in_rule__AnyValues__Group_3__05574 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_rule__AnyValues__Group_3__0__Impl5601 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_3__1__Impl_in_rule__AnyValues__Group_3__15630 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_3__2_in_rule__AnyValues__Group_3__15633 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_16_in_rule__AnyValues__Group_3__1__Impl5661 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__AnyValues__Group_3__2__Impl_in_rule__AnyValues__Group_3__25692 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_rule__AnyValues__Group_3__2__Impl5719 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__SlashPath__Group__0__Impl_in_rule__SlashPath__Group__05757 = new BitSet(new long[]{0x0000000000002000L});
    public static final BitSet FOLLOW_rule__SlashPath__Group__1_in_rule__SlashPath__Group__05760 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__SlashPath__Group__0__Impl5787 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__SlashPath__Group__1__Impl_in_rule__SlashPath__Group__15816 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__SlashPath__Group_1__0_in_rule__SlashPath__Group__1__Impl5843 = new BitSet(new long[]{0x0000000000002002L});
    public static final BitSet FOLLOW_rule__SlashPath__Group_1__0__Impl_in_rule__SlashPath__Group_1__05878 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_rule__SlashPath__Group_1__1_in_rule__SlashPath__Group_1__05881 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_13_in_rule__SlashPath__Group_1__0__Impl5909 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__SlashPath__Group_1__1__Impl_in_rule__SlashPath__Group_1__15940 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__SlashPath__Group_1__1__Impl5967 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleAnalysisElement_in_rule__ModelContainer__NameAssignment_06008 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterContainer_in_rule__ModelContainer__ParameterAssignment_16039 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuesContainer_in_rule__ModelContainer__IssuesAssignment_26070 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParametersElement_in_rule__ParameterContainer__NameAssignment_06101 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterElement_in_rule__ParameterContainer__ElementsAssignment_16132 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__ParameterKeyValueElement__NameAssignment_06163 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleAnyValues_in_rule__ParameterKeyValueElement__ValueAssignment_26194 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_rule__ParameterKeyElement__NameAssignment_06225 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueElement_in_rule__IssuesContainer__ElementsAssignment_16256 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_rule__IssuesTitleElement__NameAssignment6292 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueTypes_in_rule__IssueElement__NameAssignment_06331 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueTypes_in_rule__IssueElement__NameAssignment_1_16362 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueSuppressComment_in_rule__IssueElement__CommentAssignment_26393 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_STRING_in_rule__IssueElement__MessageAssignment_46424 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategoryElement_in_rule__IssueElement__CategoriesAssignment_56455 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueKindElement_in_rule__IssueElement__KindsAssignment_66486 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueRelevanceElement_in_rule__IssueElement__RelevanceAssignment_76517 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuePackageElement_in_rule__IssueElement__PackageAssignment_86548 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueClassElement_in_rule__IssueElement__ClassAssignment_96579 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_rule__IssueSuppressComment__ValueAlternatives_1_0_in_rule__IssueSuppressComment__ValueAssignment_16610 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_rule__IssueCategoryElement__NameAssignment_06648 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategories_in_rule__IssueCategoryElement__ElementsAssignment_16687 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategories_in_rule__IssueCategoryElement__ElementsAssignment_2_16718 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_rule__IssueKindElement__NameAssignment_06754 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueKinds_in_rule__IssueKindElement__ElementsAssignment_16793 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueKinds_in_rule__IssueKindElement__ElementsAssignment_2_16824 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_rule__IssueRelevanceElement__NameAssignment_06860 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_rule__IssueRelevanceElement__RelevanceAssignment_16899 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_rule__IssuePackageElement__NameAssignment_06935 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleSlashPath_in_rule__IssuePackageElement__PackageAssignment_16974 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_rule__IssueClassElement__NameAssignment_07010 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueClass_in_rule__IssueClassElement__ClassAssignment_17049 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_rule__IssueCategories__BugAssignment_07085 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_rule__IssueCategories__SmellAssignment_17129 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_44_in_rule__IssueCategories__PerformanceAssignment_27173 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_45_in_rule__IssueCategories__ComprehensibilityAssignment_37217 = new BitSet(new long[]{0x0000000000000002L});

}