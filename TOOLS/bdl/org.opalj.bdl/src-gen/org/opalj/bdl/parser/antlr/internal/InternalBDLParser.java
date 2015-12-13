package org.opalj.bdl.parser.antlr.internal; 

import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.*;
import org.eclipse.xtext.parser.impl.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.antlr.AbstractInternalAntlrParser;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
import org.eclipse.xtext.parser.antlr.AntlrDatatypeRuleToken;
import org.opalj.bdl.services.BDLGrammarAccess;



import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("all")
public class InternalBDLParser extends AbstractInternalAntlrParser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "RULE_ID", "RULE_STRING", "RULE_INT", "RULE_ML_COMMENT", "RULE_SL_COMMENT", "RULE_WS", "RULE_ANY_OTHER", "'Analysis of '", "':'", "'/'", "'\\\\'", "','", "'.'", "'Parameters'", "'='", "';'", "'Issues'", "'{'", "'}'", "'[suppress='", "']'", "'Categories:'", "'Kinds:'", "'Relevance:'", "'Package:'", "'Class:'", "'DeadEdgesAnalysis'", "'UnusedLocalVariables'", "'GuardedAndUnguardedAccessAnalysis'", "'UnusedMethodsAnalysis'", "'UselessComputationsAnalysis'", "'bug'", "'smell'", "'performance'", "'comprehensibility'", "'constant computation'", "'dead path'", "'throws exception'", "'unguarded use'", "'unused'", "'useless'", "'$'"
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
    public String getGrammarFileName() { return "../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g"; }



     	private BDLGrammarAccess grammarAccess;
     	
        public InternalBDLParser(TokenStream input, BDLGrammarAccess grammarAccess) {
            this(input);
            this.grammarAccess = grammarAccess;
            registerRules(grammarAccess.getGrammar());
        }
        
        @Override
        protected String getFirstRuleName() {
        	return "Model";	
       	}
       	
       	@Override
       	protected BDLGrammarAccess getGrammarAccess() {
       		return grammarAccess;
       	}



    // $ANTLR start "entryRuleModel"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:67:1: entryRuleModel returns [EObject current=null] : iv_ruleModel= ruleModel EOF ;
    public final EObject entryRuleModel() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleModel = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:68:2: (iv_ruleModel= ruleModel EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:69:2: iv_ruleModel= ruleModel EOF
            {
             newCompositeNode(grammarAccess.getModelRule()); 
            pushFollow(FOLLOW_ruleModel_in_entryRuleModel75);
            iv_ruleModel=ruleModel();

            state._fsp--;

             current =iv_ruleModel; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleModel85); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleModel"


    // $ANTLR start "ruleModel"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:76:1: ruleModel returns [EObject current=null] : this_ModelContainer_0= ruleModelContainer ;
    public final EObject ruleModel() throws RecognitionException {
        EObject current = null;

        EObject this_ModelContainer_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:79:28: (this_ModelContainer_0= ruleModelContainer )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:81:5: this_ModelContainer_0= ruleModelContainer
            {
             
                    newCompositeNode(grammarAccess.getModelAccess().getModelContainerParserRuleCall()); 
                
            pushFollow(FOLLOW_ruleModelContainer_in_ruleModel131);
            this_ModelContainer_0=ruleModelContainer();

            state._fsp--;

             
                    current = this_ModelContainer_0; 
                    afterParserOrEnumRuleCall();
                

            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleModel"


    // $ANTLR start "entryRuleModelContainer"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:97:1: entryRuleModelContainer returns [EObject current=null] : iv_ruleModelContainer= ruleModelContainer EOF ;
    public final EObject entryRuleModelContainer() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleModelContainer = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:98:2: (iv_ruleModelContainer= ruleModelContainer EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:99:2: iv_ruleModelContainer= ruleModelContainer EOF
            {
             newCompositeNode(grammarAccess.getModelContainerRule()); 
            pushFollow(FOLLOW_ruleModelContainer_in_entryRuleModelContainer165);
            iv_ruleModelContainer=ruleModelContainer();

            state._fsp--;

             current =iv_ruleModelContainer; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleModelContainer175); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleModelContainer"


    // $ANTLR start "ruleModelContainer"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:106:1: ruleModelContainer returns [EObject current=null] : ( ( (lv_name_0_0= ruleAnalysisElement ) ) ( (lv_parameter_1_0= ruleParameterContainer ) ) ( (lv_issues_2_0= ruleIssuesContainer ) ) ) ;
    public final EObject ruleModelContainer() throws RecognitionException {
        EObject current = null;

        AntlrDatatypeRuleToken lv_name_0_0 = null;

        EObject lv_parameter_1_0 = null;

        EObject lv_issues_2_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:109:28: ( ( ( (lv_name_0_0= ruleAnalysisElement ) ) ( (lv_parameter_1_0= ruleParameterContainer ) ) ( (lv_issues_2_0= ruleIssuesContainer ) ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:110:1: ( ( (lv_name_0_0= ruleAnalysisElement ) ) ( (lv_parameter_1_0= ruleParameterContainer ) ) ( (lv_issues_2_0= ruleIssuesContainer ) ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:110:1: ( ( (lv_name_0_0= ruleAnalysisElement ) ) ( (lv_parameter_1_0= ruleParameterContainer ) ) ( (lv_issues_2_0= ruleIssuesContainer ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:110:2: ( (lv_name_0_0= ruleAnalysisElement ) ) ( (lv_parameter_1_0= ruleParameterContainer ) ) ( (lv_issues_2_0= ruleIssuesContainer ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:110:2: ( (lv_name_0_0= ruleAnalysisElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:111:1: (lv_name_0_0= ruleAnalysisElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:111:1: (lv_name_0_0= ruleAnalysisElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:112:3: lv_name_0_0= ruleAnalysisElement
            {
             
            	        newCompositeNode(grammarAccess.getModelContainerAccess().getNameAnalysisElementParserRuleCall_0_0()); 
            	    
            pushFollow(FOLLOW_ruleAnalysisElement_in_ruleModelContainer221);
            lv_name_0_0=ruleAnalysisElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getModelContainerRule());
            	        }
                   		set(
                   			current, 
                   			"name",
                    		lv_name_0_0, 
                    		"AnalysisElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:128:2: ( (lv_parameter_1_0= ruleParameterContainer ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:129:1: (lv_parameter_1_0= ruleParameterContainer )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:129:1: (lv_parameter_1_0= ruleParameterContainer )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:130:3: lv_parameter_1_0= ruleParameterContainer
            {
             
            	        newCompositeNode(grammarAccess.getModelContainerAccess().getParameterParameterContainerParserRuleCall_1_0()); 
            	    
            pushFollow(FOLLOW_ruleParameterContainer_in_ruleModelContainer242);
            lv_parameter_1_0=ruleParameterContainer();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getModelContainerRule());
            	        }
                   		set(
                   			current, 
                   			"parameter",
                    		lv_parameter_1_0, 
                    		"ParameterContainer");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:146:2: ( (lv_issues_2_0= ruleIssuesContainer ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:147:1: (lv_issues_2_0= ruleIssuesContainer )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:147:1: (lv_issues_2_0= ruleIssuesContainer )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:148:3: lv_issues_2_0= ruleIssuesContainer
            {
             
            	        newCompositeNode(grammarAccess.getModelContainerAccess().getIssuesIssuesContainerParserRuleCall_2_0()); 
            	    
            pushFollow(FOLLOW_ruleIssuesContainer_in_ruleModelContainer263);
            lv_issues_2_0=ruleIssuesContainer();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getModelContainerRule());
            	        }
                   		set(
                   			current, 
                   			"issues",
                    		lv_issues_2_0, 
                    		"IssuesContainer");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleModelContainer"


    // $ANTLR start "entryRuleAnalysisElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:172:1: entryRuleAnalysisElement returns [String current=null] : iv_ruleAnalysisElement= ruleAnalysisElement EOF ;
    public final String entryRuleAnalysisElement() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleAnalysisElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:173:2: (iv_ruleAnalysisElement= ruleAnalysisElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:174:2: iv_ruleAnalysisElement= ruleAnalysisElement EOF
            {
             newCompositeNode(grammarAccess.getAnalysisElementRule()); 
            pushFollow(FOLLOW_ruleAnalysisElement_in_entryRuleAnalysisElement300);
            iv_ruleAnalysisElement=ruleAnalysisElement();

            state._fsp--;

             current =iv_ruleAnalysisElement.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleAnalysisElement311); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleAnalysisElement"


    // $ANTLR start "ruleAnalysisElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:181:1: ruleAnalysisElement returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : (kw= 'Analysis of ' (this_ID_1= RULE_ID | kw= ':' | kw= '/' | kw= '\\\\' | kw= ',' | kw= '.' )+ ) ;
    public final AntlrDatatypeRuleToken ruleAnalysisElement() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token kw=null;
        Token this_ID_1=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:184:28: ( (kw= 'Analysis of ' (this_ID_1= RULE_ID | kw= ':' | kw= '/' | kw= '\\\\' | kw= ',' | kw= '.' )+ ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:185:1: (kw= 'Analysis of ' (this_ID_1= RULE_ID | kw= ':' | kw= '/' | kw= '\\\\' | kw= ',' | kw= '.' )+ )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:185:1: (kw= 'Analysis of ' (this_ID_1= RULE_ID | kw= ':' | kw= '/' | kw= '\\\\' | kw= ',' | kw= '.' )+ )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:186:2: kw= 'Analysis of ' (this_ID_1= RULE_ID | kw= ':' | kw= '/' | kw= '\\\\' | kw= ',' | kw= '.' )+
            {
            kw=(Token)match(input,11,FOLLOW_11_in_ruleAnalysisElement349); 

                    current.merge(kw);
                    newLeafNode(kw, grammarAccess.getAnalysisElementAccess().getAnalysisOfKeyword_0()); 
                
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:191:1: (this_ID_1= RULE_ID | kw= ':' | kw= '/' | kw= '\\\\' | kw= ',' | kw= '.' )+
            int cnt1=0;
            loop1:
            do {
                int alt1=7;
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

                }

                switch (alt1) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:191:6: this_ID_1= RULE_ID
            	    {
            	    this_ID_1=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleAnalysisElement365); 

            	    		current.merge(this_ID_1);
            	        
            	     
            	        newLeafNode(this_ID_1, grammarAccess.getAnalysisElementAccess().getIDTerminalRuleCall_1_0()); 
            	        

            	    }
            	    break;
            	case 2 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:200:2: kw= ':'
            	    {
            	    kw=(Token)match(input,12,FOLLOW_12_in_ruleAnalysisElement389); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getAnalysisElementAccess().getColonKeyword_1_1()); 
            	        

            	    }
            	    break;
            	case 3 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:207:2: kw= '/'
            	    {
            	    kw=(Token)match(input,13,FOLLOW_13_in_ruleAnalysisElement408); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getAnalysisElementAccess().getSolidusKeyword_1_2()); 
            	        

            	    }
            	    break;
            	case 4 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:214:2: kw= '\\\\'
            	    {
            	    kw=(Token)match(input,14,FOLLOW_14_in_ruleAnalysisElement427); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getAnalysisElementAccess().getReverseSolidusKeyword_1_3()); 
            	        

            	    }
            	    break;
            	case 5 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:221:2: kw= ','
            	    {
            	    kw=(Token)match(input,15,FOLLOW_15_in_ruleAnalysisElement446); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getAnalysisElementAccess().getCommaKeyword_1_4()); 
            	        

            	    }
            	    break;
            	case 6 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:228:2: kw= '.'
            	    {
            	    kw=(Token)match(input,16,FOLLOW_16_in_ruleAnalysisElement465); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getAnalysisElementAccess().getFullStopKeyword_1_5()); 
            	        

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleAnalysisElement"


    // $ANTLR start "entryRuleParameterContainer"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:241:1: entryRuleParameterContainer returns [EObject current=null] : iv_ruleParameterContainer= ruleParameterContainer EOF ;
    public final EObject entryRuleParameterContainer() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleParameterContainer = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:242:2: (iv_ruleParameterContainer= ruleParameterContainer EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:243:2: iv_ruleParameterContainer= ruleParameterContainer EOF
            {
             newCompositeNode(grammarAccess.getParameterContainerRule()); 
            pushFollow(FOLLOW_ruleParameterContainer_in_entryRuleParameterContainer507);
            iv_ruleParameterContainer=ruleParameterContainer();

            state._fsp--;

             current =iv_ruleParameterContainer; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterContainer517); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleParameterContainer"


    // $ANTLR start "ruleParameterContainer"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:250:1: ruleParameterContainer returns [EObject current=null] : ( ( (lv_name_0_0= ruleParametersElement ) ) ( (lv_elements_1_0= ruleParameterElement ) )+ ) ;
    public final EObject ruleParameterContainer() throws RecognitionException {
        EObject current = null;

        AntlrDatatypeRuleToken lv_name_0_0 = null;

        EObject lv_elements_1_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:253:28: ( ( ( (lv_name_0_0= ruleParametersElement ) ) ( (lv_elements_1_0= ruleParameterElement ) )+ ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:254:1: ( ( (lv_name_0_0= ruleParametersElement ) ) ( (lv_elements_1_0= ruleParameterElement ) )+ )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:254:1: ( ( (lv_name_0_0= ruleParametersElement ) ) ( (lv_elements_1_0= ruleParameterElement ) )+ )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:254:2: ( (lv_name_0_0= ruleParametersElement ) ) ( (lv_elements_1_0= ruleParameterElement ) )+
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:254:2: ( (lv_name_0_0= ruleParametersElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:255:1: (lv_name_0_0= ruleParametersElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:255:1: (lv_name_0_0= ruleParametersElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:256:3: lv_name_0_0= ruleParametersElement
            {
             
            	        newCompositeNode(grammarAccess.getParameterContainerAccess().getNameParametersElementParserRuleCall_0_0()); 
            	    
            pushFollow(FOLLOW_ruleParametersElement_in_ruleParameterContainer563);
            lv_name_0_0=ruleParametersElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getParameterContainerRule());
            	        }
                   		set(
                   			current, 
                   			"name",
                    		lv_name_0_0, 
                    		"ParametersElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:272:2: ( (lv_elements_1_0= ruleParameterElement ) )+
            int cnt2=0;
            loop2:
            do {
                int alt2=2;
                int LA2_0 = input.LA(1);

                if ( (LA2_0==RULE_ID) ) {
                    alt2=1;
                }


                switch (alt2) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:273:1: (lv_elements_1_0= ruleParameterElement )
            	    {
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:273:1: (lv_elements_1_0= ruleParameterElement )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:274:3: lv_elements_1_0= ruleParameterElement
            	    {
            	     
            	    	        newCompositeNode(grammarAccess.getParameterContainerAccess().getElementsParameterElementParserRuleCall_1_0()); 
            	    	    
            	    pushFollow(FOLLOW_ruleParameterElement_in_ruleParameterContainer584);
            	    lv_elements_1_0=ruleParameterElement();

            	    state._fsp--;


            	    	        if (current==null) {
            	    	            current = createModelElementForParent(grammarAccess.getParameterContainerRule());
            	    	        }
            	           		add(
            	           			current, 
            	           			"elements",
            	            		lv_elements_1_0, 
            	            		"ParameterElement");
            	    	        afterParserOrEnumRuleCall();
            	    	    

            	    }


            	    }
            	    break;

            	default :
            	    if ( cnt2 >= 1 ) break loop2;
                        EarlyExitException eee =
                            new EarlyExitException(2, input);
                        throw eee;
                }
                cnt2++;
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleParameterContainer"


    // $ANTLR start "entryRuleParametersElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:298:1: entryRuleParametersElement returns [String current=null] : iv_ruleParametersElement= ruleParametersElement EOF ;
    public final String entryRuleParametersElement() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleParametersElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:299:2: (iv_ruleParametersElement= ruleParametersElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:300:2: iv_ruleParametersElement= ruleParametersElement EOF
            {
             newCompositeNode(grammarAccess.getParametersElementRule()); 
            pushFollow(FOLLOW_ruleParametersElement_in_entryRuleParametersElement622);
            iv_ruleParametersElement=ruleParametersElement();

            state._fsp--;

             current =iv_ruleParametersElement.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParametersElement633); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleParametersElement"


    // $ANTLR start "ruleParametersElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:307:1: ruleParametersElement returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : kw= 'Parameters' ;
    public final AntlrDatatypeRuleToken ruleParametersElement() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token kw=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:310:28: (kw= 'Parameters' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:312:2: kw= 'Parameters'
            {
            kw=(Token)match(input,17,FOLLOW_17_in_ruleParametersElement670); 

                    current.merge(kw);
                    newLeafNode(kw, grammarAccess.getParametersElementAccess().getParametersKeyword()); 
                

            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleParametersElement"


    // $ANTLR start "entryRuleParameterElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:325:1: entryRuleParameterElement returns [EObject current=null] : iv_ruleParameterElement= ruleParameterElement EOF ;
    public final EObject entryRuleParameterElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleParameterElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:326:2: (iv_ruleParameterElement= ruleParameterElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:327:2: iv_ruleParameterElement= ruleParameterElement EOF
            {
             newCompositeNode(grammarAccess.getParameterElementRule()); 
            pushFollow(FOLLOW_ruleParameterElement_in_entryRuleParameterElement709);
            iv_ruleParameterElement=ruleParameterElement();

            state._fsp--;

             current =iv_ruleParameterElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterElement719); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleParameterElement"


    // $ANTLR start "ruleParameterElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:334:1: ruleParameterElement returns [EObject current=null] : (this_ParameterKeyValueElement_0= ruleParameterKeyValueElement | this_ParameterKeyElement_1= ruleParameterKeyElement ) ;
    public final EObject ruleParameterElement() throws RecognitionException {
        EObject current = null;

        EObject this_ParameterKeyValueElement_0 = null;

        EObject this_ParameterKeyElement_1 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:337:28: ( (this_ParameterKeyValueElement_0= ruleParameterKeyValueElement | this_ParameterKeyElement_1= ruleParameterKeyElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:338:1: (this_ParameterKeyValueElement_0= ruleParameterKeyValueElement | this_ParameterKeyElement_1= ruleParameterKeyElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:338:1: (this_ParameterKeyValueElement_0= ruleParameterKeyValueElement | this_ParameterKeyElement_1= ruleParameterKeyElement )
            int alt3=2;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==RULE_ID) ) {
                int LA3_1 = input.LA(2);

                if ( (LA3_1==12||LA3_1==18) ) {
                    alt3=1;
                }
                else if ( (LA3_1==19) ) {
                    alt3=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 3, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 3, 0, input);

                throw nvae;
            }
            switch (alt3) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:339:5: this_ParameterKeyValueElement_0= ruleParameterKeyValueElement
                    {
                     
                            newCompositeNode(grammarAccess.getParameterElementAccess().getParameterKeyValueElementParserRuleCall_0()); 
                        
                    pushFollow(FOLLOW_ruleParameterKeyValueElement_in_ruleParameterElement766);
                    this_ParameterKeyValueElement_0=ruleParameterKeyValueElement();

                    state._fsp--;

                     
                            current = this_ParameterKeyValueElement_0; 
                            afterParserOrEnumRuleCall();
                        

                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:349:5: this_ParameterKeyElement_1= ruleParameterKeyElement
                    {
                     
                            newCompositeNode(grammarAccess.getParameterElementAccess().getParameterKeyElementParserRuleCall_1()); 
                        
                    pushFollow(FOLLOW_ruleParameterKeyElement_in_ruleParameterElement793);
                    this_ParameterKeyElement_1=ruleParameterKeyElement();

                    state._fsp--;

                     
                            current = this_ParameterKeyElement_1; 
                            afterParserOrEnumRuleCall();
                        

                    }
                    break;

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleParameterElement"


    // $ANTLR start "entryRuleParameterKeyValueElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:365:1: entryRuleParameterKeyValueElement returns [EObject current=null] : iv_ruleParameterKeyValueElement= ruleParameterKeyValueElement EOF ;
    public final EObject entryRuleParameterKeyValueElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleParameterKeyValueElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:366:2: (iv_ruleParameterKeyValueElement= ruleParameterKeyValueElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:367:2: iv_ruleParameterKeyValueElement= ruleParameterKeyValueElement EOF
            {
             newCompositeNode(grammarAccess.getParameterKeyValueElementRule()); 
            pushFollow(FOLLOW_ruleParameterKeyValueElement_in_entryRuleParameterKeyValueElement828);
            iv_ruleParameterKeyValueElement=ruleParameterKeyValueElement();

            state._fsp--;

             current =iv_ruleParameterKeyValueElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterKeyValueElement838); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleParameterKeyValueElement"


    // $ANTLR start "ruleParameterKeyValueElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:374:1: ruleParameterKeyValueElement returns [EObject current=null] : ( ( (lv_name_0_0= RULE_ID ) ) (otherlv_1= ':' | otherlv_2= '=' ) ( (lv_value_3_0= ruleAnyValues ) ) ) ;
    public final EObject ruleParameterKeyValueElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        Token otherlv_1=null;
        Token otherlv_2=null;
        AntlrDatatypeRuleToken lv_value_3_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:377:28: ( ( ( (lv_name_0_0= RULE_ID ) ) (otherlv_1= ':' | otherlv_2= '=' ) ( (lv_value_3_0= ruleAnyValues ) ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:378:1: ( ( (lv_name_0_0= RULE_ID ) ) (otherlv_1= ':' | otherlv_2= '=' ) ( (lv_value_3_0= ruleAnyValues ) ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:378:1: ( ( (lv_name_0_0= RULE_ID ) ) (otherlv_1= ':' | otherlv_2= '=' ) ( (lv_value_3_0= ruleAnyValues ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:378:2: ( (lv_name_0_0= RULE_ID ) ) (otherlv_1= ':' | otherlv_2= '=' ) ( (lv_value_3_0= ruleAnyValues ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:378:2: ( (lv_name_0_0= RULE_ID ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:379:1: (lv_name_0_0= RULE_ID )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:379:1: (lv_name_0_0= RULE_ID )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:380:3: lv_name_0_0= RULE_ID
            {
            lv_name_0_0=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleParameterKeyValueElement880); 

            			newLeafNode(lv_name_0_0, grammarAccess.getParameterKeyValueElementAccess().getNameIDTerminalRuleCall_0_0()); 
            		

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getParameterKeyValueElementRule());
            	        }
                   		setWithLastConsumed(
                   			current, 
                   			"name",
                    		lv_name_0_0, 
                    		"ID");
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:396:2: (otherlv_1= ':' | otherlv_2= '=' )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==12) ) {
                alt4=1;
            }
            else if ( (LA4_0==18) ) {
                alt4=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:396:4: otherlv_1= ':'
                    {
                    otherlv_1=(Token)match(input,12,FOLLOW_12_in_ruleParameterKeyValueElement898); 

                        	newLeafNode(otherlv_1, grammarAccess.getParameterKeyValueElementAccess().getColonKeyword_1_0());
                        

                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:401:7: otherlv_2= '='
                    {
                    otherlv_2=(Token)match(input,18,FOLLOW_18_in_ruleParameterKeyValueElement916); 

                        	newLeafNode(otherlv_2, grammarAccess.getParameterKeyValueElementAccess().getEqualsSignKeyword_1_1());
                        

                    }
                    break;

            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:405:2: ( (lv_value_3_0= ruleAnyValues ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:406:1: (lv_value_3_0= ruleAnyValues )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:406:1: (lv_value_3_0= ruleAnyValues )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:407:3: lv_value_3_0= ruleAnyValues
            {
             
            	        newCompositeNode(grammarAccess.getParameterKeyValueElementAccess().getValueAnyValuesParserRuleCall_2_0()); 
            	    
            pushFollow(FOLLOW_ruleAnyValues_in_ruleParameterKeyValueElement938);
            lv_value_3_0=ruleAnyValues();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getParameterKeyValueElementRule());
            	        }
                   		set(
                   			current, 
                   			"value",
                    		lv_value_3_0, 
                    		"AnyValues");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleParameterKeyValueElement"


    // $ANTLR start "entryRuleParameterKeyElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:431:1: entryRuleParameterKeyElement returns [EObject current=null] : iv_ruleParameterKeyElement= ruleParameterKeyElement EOF ;
    public final EObject entryRuleParameterKeyElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleParameterKeyElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:432:2: (iv_ruleParameterKeyElement= ruleParameterKeyElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:433:2: iv_ruleParameterKeyElement= ruleParameterKeyElement EOF
            {
             newCompositeNode(grammarAccess.getParameterKeyElementRule()); 
            pushFollow(FOLLOW_ruleParameterKeyElement_in_entryRuleParameterKeyElement974);
            iv_ruleParameterKeyElement=ruleParameterKeyElement();

            state._fsp--;

             current =iv_ruleParameterKeyElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleParameterKeyElement984); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleParameterKeyElement"


    // $ANTLR start "ruleParameterKeyElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:440:1: ruleParameterKeyElement returns [EObject current=null] : ( ( (lv_name_0_0= RULE_ID ) ) otherlv_1= ';' ) ;
    public final EObject ruleParameterKeyElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        Token otherlv_1=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:443:28: ( ( ( (lv_name_0_0= RULE_ID ) ) otherlv_1= ';' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:444:1: ( ( (lv_name_0_0= RULE_ID ) ) otherlv_1= ';' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:444:1: ( ( (lv_name_0_0= RULE_ID ) ) otherlv_1= ';' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:444:2: ( (lv_name_0_0= RULE_ID ) ) otherlv_1= ';'
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:444:2: ( (lv_name_0_0= RULE_ID ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:445:1: (lv_name_0_0= RULE_ID )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:445:1: (lv_name_0_0= RULE_ID )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:446:3: lv_name_0_0= RULE_ID
            {
            lv_name_0_0=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleParameterKeyElement1026); 

            			newLeafNode(lv_name_0_0, grammarAccess.getParameterKeyElementAccess().getNameIDTerminalRuleCall_0_0()); 
            		

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getParameterKeyElementRule());
            	        }
                   		setWithLastConsumed(
                   			current, 
                   			"name",
                    		lv_name_0_0, 
                    		"ID");
            	    

            }


            }

            otherlv_1=(Token)match(input,19,FOLLOW_19_in_ruleParameterKeyElement1043); 

                	newLeafNode(otherlv_1, grammarAccess.getParameterKeyElementAccess().getSemicolonKeyword_1());
                

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleParameterKeyElement"


    // $ANTLR start "entryRuleIssuesContainer"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:474:1: entryRuleIssuesContainer returns [EObject current=null] : iv_ruleIssuesContainer= ruleIssuesContainer EOF ;
    public final EObject entryRuleIssuesContainer() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssuesContainer = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:475:2: (iv_ruleIssuesContainer= ruleIssuesContainer EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:476:2: iv_ruleIssuesContainer= ruleIssuesContainer EOF
            {
             newCompositeNode(grammarAccess.getIssuesContainerRule()); 
            pushFollow(FOLLOW_ruleIssuesContainer_in_entryRuleIssuesContainer1079);
            iv_ruleIssuesContainer=ruleIssuesContainer();

            state._fsp--;

             current =iv_ruleIssuesContainer; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssuesContainer1089); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssuesContainer"


    // $ANTLR start "ruleIssuesContainer"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:483:1: ruleIssuesContainer returns [EObject current=null] : (this_IssuesTitleElement_0= ruleIssuesTitleElement ( (lv_elements_1_0= ruleIssueElement ) )* ) ;
    public final EObject ruleIssuesContainer() throws RecognitionException {
        EObject current = null;

        EObject this_IssuesTitleElement_0 = null;

        EObject lv_elements_1_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:486:28: ( (this_IssuesTitleElement_0= ruleIssuesTitleElement ( (lv_elements_1_0= ruleIssueElement ) )* ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:487:1: (this_IssuesTitleElement_0= ruleIssuesTitleElement ( (lv_elements_1_0= ruleIssueElement ) )* )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:487:1: (this_IssuesTitleElement_0= ruleIssuesTitleElement ( (lv_elements_1_0= ruleIssueElement ) )* )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:488:5: this_IssuesTitleElement_0= ruleIssuesTitleElement ( (lv_elements_1_0= ruleIssueElement ) )*
            {
             
                    newCompositeNode(grammarAccess.getIssuesContainerAccess().getIssuesTitleElementParserRuleCall_0()); 
                
            pushFollow(FOLLOW_ruleIssuesTitleElement_in_ruleIssuesContainer1136);
            this_IssuesTitleElement_0=ruleIssuesTitleElement();

            state._fsp--;

             
                    current = this_IssuesTitleElement_0; 
                    afterParserOrEnumRuleCall();
                
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:496:1: ( (lv_elements_1_0= ruleIssueElement ) )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( ((LA5_0>=30 && LA5_0<=34)) ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:497:1: (lv_elements_1_0= ruleIssueElement )
            	    {
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:497:1: (lv_elements_1_0= ruleIssueElement )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:498:3: lv_elements_1_0= ruleIssueElement
            	    {
            	     
            	    	        newCompositeNode(grammarAccess.getIssuesContainerAccess().getElementsIssueElementParserRuleCall_1_0()); 
            	    	    
            	    pushFollow(FOLLOW_ruleIssueElement_in_ruleIssuesContainer1156);
            	    lv_elements_1_0=ruleIssueElement();

            	    state._fsp--;


            	    	        if (current==null) {
            	    	            current = createModelElementForParent(grammarAccess.getIssuesContainerRule());
            	    	        }
            	           		add(
            	           			current, 
            	           			"elements",
            	            		lv_elements_1_0, 
            	            		"IssueElement");
            	    	        afterParserOrEnumRuleCall();
            	    	    

            	    }


            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssuesContainer"


    // $ANTLR start "entryRuleIssuesTitleElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:522:1: entryRuleIssuesTitleElement returns [EObject current=null] : iv_ruleIssuesTitleElement= ruleIssuesTitleElement EOF ;
    public final EObject entryRuleIssuesTitleElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssuesTitleElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:523:2: (iv_ruleIssuesTitleElement= ruleIssuesTitleElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:524:2: iv_ruleIssuesTitleElement= ruleIssuesTitleElement EOF
            {
             newCompositeNode(grammarAccess.getIssuesTitleElementRule()); 
            pushFollow(FOLLOW_ruleIssuesTitleElement_in_entryRuleIssuesTitleElement1193);
            iv_ruleIssuesTitleElement=ruleIssuesTitleElement();

            state._fsp--;

             current =iv_ruleIssuesTitleElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssuesTitleElement1203); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssuesTitleElement"


    // $ANTLR start "ruleIssuesTitleElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:531:1: ruleIssuesTitleElement returns [EObject current=null] : ( (lv_name_0_0= 'Issues' ) ) ;
    public final EObject ruleIssuesTitleElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:534:28: ( ( (lv_name_0_0= 'Issues' ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:535:1: ( (lv_name_0_0= 'Issues' ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:535:1: ( (lv_name_0_0= 'Issues' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:536:1: (lv_name_0_0= 'Issues' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:536:1: (lv_name_0_0= 'Issues' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:537:3: lv_name_0_0= 'Issues'
            {
            lv_name_0_0=(Token)match(input,20,FOLLOW_20_in_ruleIssuesTitleElement1245); 

                    newLeafNode(lv_name_0_0, grammarAccess.getIssuesTitleElementAccess().getNameIssuesKeyword_0());
                

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssuesTitleElementRule());
            	        }
                   		setWithLastConsumed(current, "name", lv_name_0_0, "Issues");
            	    

            }


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssuesTitleElement"


    // $ANTLR start "entryRuleIssueElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:558:1: entryRuleIssueElement returns [EObject current=null] : iv_ruleIssueElement= ruleIssueElement EOF ;
    public final EObject entryRuleIssueElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:559:2: (iv_ruleIssueElement= ruleIssueElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:560:2: iv_ruleIssueElement= ruleIssueElement EOF
            {
             newCompositeNode(grammarAccess.getIssueElementRule()); 
            pushFollow(FOLLOW_ruleIssueElement_in_entryRuleIssueElement1293);
            iv_ruleIssueElement=ruleIssueElement();

            state._fsp--;

             current =iv_ruleIssueElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueElement1303); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueElement"


    // $ANTLR start "ruleIssueElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:567:1: ruleIssueElement returns [EObject current=null] : ( ( (lv_name_0_0= ruleIssueTypes ) ) (otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) ) )* ( (lv_comment_3_0= ruleIssueSuppressComment ) )? otherlv_4= '{' ( (lv_message_5_0= RULE_STRING ) )? ( (lv_categories_6_0= ruleIssueCategoryElement ) ) ( (lv_kinds_7_0= ruleIssueKindElement ) ) ( (lv_relevance_8_0= ruleIssueRelevanceElement ) ) ( (lv_package_9_0= ruleIssuePackageElement ) ) ( (lv_class_10_0= ruleIssueClassElement ) ) otherlv_11= '}' ) ;
    public final EObject ruleIssueElement() throws RecognitionException {
        EObject current = null;

        Token otherlv_1=null;
        Token otherlv_4=null;
        Token lv_message_5_0=null;
        Token otherlv_11=null;
        AntlrDatatypeRuleToken lv_name_0_0 = null;

        AntlrDatatypeRuleToken lv_name_2_0 = null;

        EObject lv_comment_3_0 = null;

        EObject lv_categories_6_0 = null;

        EObject lv_kinds_7_0 = null;

        EObject lv_relevance_8_0 = null;

        EObject lv_package_9_0 = null;

        EObject lv_class_10_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:570:28: ( ( ( (lv_name_0_0= ruleIssueTypes ) ) (otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) ) )* ( (lv_comment_3_0= ruleIssueSuppressComment ) )? otherlv_4= '{' ( (lv_message_5_0= RULE_STRING ) )? ( (lv_categories_6_0= ruleIssueCategoryElement ) ) ( (lv_kinds_7_0= ruleIssueKindElement ) ) ( (lv_relevance_8_0= ruleIssueRelevanceElement ) ) ( (lv_package_9_0= ruleIssuePackageElement ) ) ( (lv_class_10_0= ruleIssueClassElement ) ) otherlv_11= '}' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:571:1: ( ( (lv_name_0_0= ruleIssueTypes ) ) (otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) ) )* ( (lv_comment_3_0= ruleIssueSuppressComment ) )? otherlv_4= '{' ( (lv_message_5_0= RULE_STRING ) )? ( (lv_categories_6_0= ruleIssueCategoryElement ) ) ( (lv_kinds_7_0= ruleIssueKindElement ) ) ( (lv_relevance_8_0= ruleIssueRelevanceElement ) ) ( (lv_package_9_0= ruleIssuePackageElement ) ) ( (lv_class_10_0= ruleIssueClassElement ) ) otherlv_11= '}' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:571:1: ( ( (lv_name_0_0= ruleIssueTypes ) ) (otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) ) )* ( (lv_comment_3_0= ruleIssueSuppressComment ) )? otherlv_4= '{' ( (lv_message_5_0= RULE_STRING ) )? ( (lv_categories_6_0= ruleIssueCategoryElement ) ) ( (lv_kinds_7_0= ruleIssueKindElement ) ) ( (lv_relevance_8_0= ruleIssueRelevanceElement ) ) ( (lv_package_9_0= ruleIssuePackageElement ) ) ( (lv_class_10_0= ruleIssueClassElement ) ) otherlv_11= '}' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:571:2: ( (lv_name_0_0= ruleIssueTypes ) ) (otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) ) )* ( (lv_comment_3_0= ruleIssueSuppressComment ) )? otherlv_4= '{' ( (lv_message_5_0= RULE_STRING ) )? ( (lv_categories_6_0= ruleIssueCategoryElement ) ) ( (lv_kinds_7_0= ruleIssueKindElement ) ) ( (lv_relevance_8_0= ruleIssueRelevanceElement ) ) ( (lv_package_9_0= ruleIssuePackageElement ) ) ( (lv_class_10_0= ruleIssueClassElement ) ) otherlv_11= '}'
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:571:2: ( (lv_name_0_0= ruleIssueTypes ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:572:1: (lv_name_0_0= ruleIssueTypes )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:572:1: (lv_name_0_0= ruleIssueTypes )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:573:3: lv_name_0_0= ruleIssueTypes
            {
             
            	        newCompositeNode(grammarAccess.getIssueElementAccess().getNameIssueTypesParserRuleCall_0_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueTypes_in_ruleIssueElement1349);
            lv_name_0_0=ruleIssueTypes();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	        }
                   		add(
                   			current, 
                   			"name",
                    		lv_name_0_0, 
                    		"IssueTypes");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:589:2: (otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) ) )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( (LA6_0==15) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:589:4: otherlv_1= ',' ( (lv_name_2_0= ruleIssueTypes ) )
            	    {
            	    otherlv_1=(Token)match(input,15,FOLLOW_15_in_ruleIssueElement1362); 

            	        	newLeafNode(otherlv_1, grammarAccess.getIssueElementAccess().getCommaKeyword_1_0());
            	        
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:593:1: ( (lv_name_2_0= ruleIssueTypes ) )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:594:1: (lv_name_2_0= ruleIssueTypes )
            	    {
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:594:1: (lv_name_2_0= ruleIssueTypes )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:595:3: lv_name_2_0= ruleIssueTypes
            	    {
            	     
            	    	        newCompositeNode(grammarAccess.getIssueElementAccess().getNameIssueTypesParserRuleCall_1_1_0()); 
            	    	    
            	    pushFollow(FOLLOW_ruleIssueTypes_in_ruleIssueElement1383);
            	    lv_name_2_0=ruleIssueTypes();

            	    state._fsp--;


            	    	        if (current==null) {
            	    	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	    	        }
            	           		add(
            	           			current, 
            	           			"name",
            	            		lv_name_2_0, 
            	            		"IssueTypes");
            	    	        afterParserOrEnumRuleCall();
            	    	    

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:611:4: ( (lv_comment_3_0= ruleIssueSuppressComment ) )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==23) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:612:1: (lv_comment_3_0= ruleIssueSuppressComment )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:612:1: (lv_comment_3_0= ruleIssueSuppressComment )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:613:3: lv_comment_3_0= ruleIssueSuppressComment
                    {
                     
                    	        newCompositeNode(grammarAccess.getIssueElementAccess().getCommentIssueSuppressCommentParserRuleCall_2_0()); 
                    	    
                    pushFollow(FOLLOW_ruleIssueSuppressComment_in_ruleIssueElement1406);
                    lv_comment_3_0=ruleIssueSuppressComment();

                    state._fsp--;


                    	        if (current==null) {
                    	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
                    	        }
                           		set(
                           			current, 
                           			"comment",
                            		lv_comment_3_0, 
                            		"IssueSuppressComment");
                    	        afterParserOrEnumRuleCall();
                    	    

                    }


                    }
                    break;

            }

            otherlv_4=(Token)match(input,21,FOLLOW_21_in_ruleIssueElement1419); 

                	newLeafNode(otherlv_4, grammarAccess.getIssueElementAccess().getLeftCurlyBracketKeyword_3());
                
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:633:1: ( (lv_message_5_0= RULE_STRING ) )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==RULE_STRING) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:634:1: (lv_message_5_0= RULE_STRING )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:634:1: (lv_message_5_0= RULE_STRING )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:635:3: lv_message_5_0= RULE_STRING
                    {
                    lv_message_5_0=(Token)match(input,RULE_STRING,FOLLOW_RULE_STRING_in_ruleIssueElement1436); 

                    			newLeafNode(lv_message_5_0, grammarAccess.getIssueElementAccess().getMessageSTRINGTerminalRuleCall_4_0()); 
                    		

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueElementRule());
                    	        }
                           		setWithLastConsumed(
                           			current, 
                           			"message",
                            		lv_message_5_0, 
                            		"STRING");
                    	    

                    }


                    }
                    break;

            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:651:3: ( (lv_categories_6_0= ruleIssueCategoryElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:652:1: (lv_categories_6_0= ruleIssueCategoryElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:652:1: (lv_categories_6_0= ruleIssueCategoryElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:653:3: lv_categories_6_0= ruleIssueCategoryElement
            {
             
            	        newCompositeNode(grammarAccess.getIssueElementAccess().getCategoriesIssueCategoryElementParserRuleCall_5_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueCategoryElement_in_ruleIssueElement1463);
            lv_categories_6_0=ruleIssueCategoryElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	        }
                   		set(
                   			current, 
                   			"categories",
                    		lv_categories_6_0, 
                    		"IssueCategoryElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:669:2: ( (lv_kinds_7_0= ruleIssueKindElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:670:1: (lv_kinds_7_0= ruleIssueKindElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:670:1: (lv_kinds_7_0= ruleIssueKindElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:671:3: lv_kinds_7_0= ruleIssueKindElement
            {
             
            	        newCompositeNode(grammarAccess.getIssueElementAccess().getKindsIssueKindElementParserRuleCall_6_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueKindElement_in_ruleIssueElement1484);
            lv_kinds_7_0=ruleIssueKindElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	        }
                   		set(
                   			current, 
                   			"kinds",
                    		lv_kinds_7_0, 
                    		"IssueKindElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:687:2: ( (lv_relevance_8_0= ruleIssueRelevanceElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:688:1: (lv_relevance_8_0= ruleIssueRelevanceElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:688:1: (lv_relevance_8_0= ruleIssueRelevanceElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:689:3: lv_relevance_8_0= ruleIssueRelevanceElement
            {
             
            	        newCompositeNode(grammarAccess.getIssueElementAccess().getRelevanceIssueRelevanceElementParserRuleCall_7_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueRelevanceElement_in_ruleIssueElement1505);
            lv_relevance_8_0=ruleIssueRelevanceElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	        }
                   		set(
                   			current, 
                   			"relevance",
                    		lv_relevance_8_0, 
                    		"IssueRelevanceElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:705:2: ( (lv_package_9_0= ruleIssuePackageElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:706:1: (lv_package_9_0= ruleIssuePackageElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:706:1: (lv_package_9_0= ruleIssuePackageElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:707:3: lv_package_9_0= ruleIssuePackageElement
            {
             
            	        newCompositeNode(grammarAccess.getIssueElementAccess().getPackageIssuePackageElementParserRuleCall_8_0()); 
            	    
            pushFollow(FOLLOW_ruleIssuePackageElement_in_ruleIssueElement1526);
            lv_package_9_0=ruleIssuePackageElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	        }
                   		set(
                   			current, 
                   			"package",
                    		lv_package_9_0, 
                    		"IssuePackageElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:723:2: ( (lv_class_10_0= ruleIssueClassElement ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:724:1: (lv_class_10_0= ruleIssueClassElement )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:724:1: (lv_class_10_0= ruleIssueClassElement )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:725:3: lv_class_10_0= ruleIssueClassElement
            {
             
            	        newCompositeNode(grammarAccess.getIssueElementAccess().getClassIssueClassElementParserRuleCall_9_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueClassElement_in_ruleIssueElement1547);
            lv_class_10_0=ruleIssueClassElement();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueElementRule());
            	        }
                   		set(
                   			current, 
                   			"class",
                    		lv_class_10_0, 
                    		"IssueClassElement");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            otherlv_11=(Token)match(input,22,FOLLOW_22_in_ruleIssueElement1559); 

                	newLeafNode(otherlv_11, grammarAccess.getIssueElementAccess().getRightCurlyBracketKeyword_10());
                

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueElement"


    // $ANTLR start "entryRuleIssueSuppressComment"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:753:1: entryRuleIssueSuppressComment returns [EObject current=null] : iv_ruleIssueSuppressComment= ruleIssueSuppressComment EOF ;
    public final EObject entryRuleIssueSuppressComment() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueSuppressComment = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:754:2: (iv_ruleIssueSuppressComment= ruleIssueSuppressComment EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:755:2: iv_ruleIssueSuppressComment= ruleIssueSuppressComment EOF
            {
             newCompositeNode(grammarAccess.getIssueSuppressCommentRule()); 
            pushFollow(FOLLOW_ruleIssueSuppressComment_in_entryRuleIssueSuppressComment1595);
            iv_ruleIssueSuppressComment=ruleIssueSuppressComment();

            state._fsp--;

             current =iv_ruleIssueSuppressComment; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueSuppressComment1605); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueSuppressComment"


    // $ANTLR start "ruleIssueSuppressComment"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:762:1: ruleIssueSuppressComment returns [EObject current=null] : (otherlv_0= '[suppress=' ( ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) ) ) otherlv_2= ']' ) ;
    public final EObject ruleIssueSuppressComment() throws RecognitionException {
        EObject current = null;

        Token otherlv_0=null;
        Token lv_value_1_1=null;
        Token lv_value_1_2=null;
        Token otherlv_2=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:765:28: ( (otherlv_0= '[suppress=' ( ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) ) ) otherlv_2= ']' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:766:1: (otherlv_0= '[suppress=' ( ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) ) ) otherlv_2= ']' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:766:1: (otherlv_0= '[suppress=' ( ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) ) ) otherlv_2= ']' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:766:3: otherlv_0= '[suppress=' ( ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) ) ) otherlv_2= ']'
            {
            otherlv_0=(Token)match(input,23,FOLLOW_23_in_ruleIssueSuppressComment1642); 

                	newLeafNode(otherlv_0, grammarAccess.getIssueSuppressCommentAccess().getSuppressKeyword_0());
                
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:770:1: ( ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:771:1: ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:771:1: ( (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:772:1: (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:772:1: (lv_value_1_1= RULE_ID | lv_value_1_2= RULE_STRING )
            int alt9=2;
            int LA9_0 = input.LA(1);

            if ( (LA9_0==RULE_ID) ) {
                alt9=1;
            }
            else if ( (LA9_0==RULE_STRING) ) {
                alt9=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 9, 0, input);

                throw nvae;
            }
            switch (alt9) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:773:3: lv_value_1_1= RULE_ID
                    {
                    lv_value_1_1=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleIssueSuppressComment1661); 

                    			newLeafNode(lv_value_1_1, grammarAccess.getIssueSuppressCommentAccess().getValueIDTerminalRuleCall_1_0_0()); 
                    		

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueSuppressCommentRule());
                    	        }
                           		setWithLastConsumed(
                           			current, 
                           			"value",
                            		lv_value_1_1, 
                            		"ID");
                    	    

                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:788:8: lv_value_1_2= RULE_STRING
                    {
                    lv_value_1_2=(Token)match(input,RULE_STRING,FOLLOW_RULE_STRING_in_ruleIssueSuppressComment1681); 

                    			newLeafNode(lv_value_1_2, grammarAccess.getIssueSuppressCommentAccess().getValueSTRINGTerminalRuleCall_1_0_1()); 
                    		

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueSuppressCommentRule());
                    	        }
                           		setWithLastConsumed(
                           			current, 
                           			"value",
                            		lv_value_1_2, 
                            		"STRING");
                    	    

                    }
                    break;

            }


            }


            }

            otherlv_2=(Token)match(input,24,FOLLOW_24_in_ruleIssueSuppressComment1701); 

                	newLeafNode(otherlv_2, grammarAccess.getIssueSuppressCommentAccess().getRightSquareBracketKeyword_2());
                

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueSuppressComment"


    // $ANTLR start "entryRuleIssueCategoryElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:818:1: entryRuleIssueCategoryElement returns [EObject current=null] : iv_ruleIssueCategoryElement= ruleIssueCategoryElement EOF ;
    public final EObject entryRuleIssueCategoryElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueCategoryElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:819:2: (iv_ruleIssueCategoryElement= ruleIssueCategoryElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:820:2: iv_ruleIssueCategoryElement= ruleIssueCategoryElement EOF
            {
             newCompositeNode(grammarAccess.getIssueCategoryElementRule()); 
            pushFollow(FOLLOW_ruleIssueCategoryElement_in_entryRuleIssueCategoryElement1737);
            iv_ruleIssueCategoryElement=ruleIssueCategoryElement();

            state._fsp--;

             current =iv_ruleIssueCategoryElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueCategoryElement1747); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueCategoryElement"


    // $ANTLR start "ruleIssueCategoryElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:827:1: ruleIssueCategoryElement returns [EObject current=null] : ( ( (lv_name_0_0= 'Categories:' ) ) ( (lv_elements_1_0= ruleIssueCategories ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) ) )* ) ;
    public final EObject ruleIssueCategoryElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        Token otherlv_2=null;
        EObject lv_elements_1_0 = null;

        EObject lv_elements_3_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:830:28: ( ( ( (lv_name_0_0= 'Categories:' ) ) ( (lv_elements_1_0= ruleIssueCategories ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) ) )* ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:831:1: ( ( (lv_name_0_0= 'Categories:' ) ) ( (lv_elements_1_0= ruleIssueCategories ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) ) )* )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:831:1: ( ( (lv_name_0_0= 'Categories:' ) ) ( (lv_elements_1_0= ruleIssueCategories ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) ) )* )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:831:2: ( (lv_name_0_0= 'Categories:' ) ) ( (lv_elements_1_0= ruleIssueCategories ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) ) )*
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:831:2: ( (lv_name_0_0= 'Categories:' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:832:1: (lv_name_0_0= 'Categories:' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:832:1: (lv_name_0_0= 'Categories:' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:833:3: lv_name_0_0= 'Categories:'
            {
            lv_name_0_0=(Token)match(input,25,FOLLOW_25_in_ruleIssueCategoryElement1790); 

                    newLeafNode(lv_name_0_0, grammarAccess.getIssueCategoryElementAccess().getNameCategoriesKeyword_0_0());
                

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssueCategoryElementRule());
            	        }
                   		setWithLastConsumed(current, "name", lv_name_0_0, "Categories:");
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:846:2: ( (lv_elements_1_0= ruleIssueCategories ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:847:1: (lv_elements_1_0= ruleIssueCategories )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:847:1: (lv_elements_1_0= ruleIssueCategories )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:848:3: lv_elements_1_0= ruleIssueCategories
            {
             
            	        newCompositeNode(grammarAccess.getIssueCategoryElementAccess().getElementsIssueCategoriesParserRuleCall_1_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueCategories_in_ruleIssueCategoryElement1824);
            lv_elements_1_0=ruleIssueCategories();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueCategoryElementRule());
            	        }
                   		add(
                   			current, 
                   			"elements",
                    		lv_elements_1_0, 
                    		"IssueCategories");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:864:2: (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) ) )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==15) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:864:4: otherlv_2= ',' ( (lv_elements_3_0= ruleIssueCategories ) )
            	    {
            	    otherlv_2=(Token)match(input,15,FOLLOW_15_in_ruleIssueCategoryElement1837); 

            	        	newLeafNode(otherlv_2, grammarAccess.getIssueCategoryElementAccess().getCommaKeyword_2_0());
            	        
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:868:1: ( (lv_elements_3_0= ruleIssueCategories ) )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:869:1: (lv_elements_3_0= ruleIssueCategories )
            	    {
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:869:1: (lv_elements_3_0= ruleIssueCategories )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:870:3: lv_elements_3_0= ruleIssueCategories
            	    {
            	     
            	    	        newCompositeNode(grammarAccess.getIssueCategoryElementAccess().getElementsIssueCategoriesParserRuleCall_2_1_0()); 
            	    	    
            	    pushFollow(FOLLOW_ruleIssueCategories_in_ruleIssueCategoryElement1858);
            	    lv_elements_3_0=ruleIssueCategories();

            	    state._fsp--;


            	    	        if (current==null) {
            	    	            current = createModelElementForParent(grammarAccess.getIssueCategoryElementRule());
            	    	        }
            	           		add(
            	           			current, 
            	           			"elements",
            	            		lv_elements_3_0, 
            	            		"IssueCategories");
            	    	        afterParserOrEnumRuleCall();
            	    	    

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueCategoryElement"


    // $ANTLR start "entryRuleIssueKindElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:894:1: entryRuleIssueKindElement returns [EObject current=null] : iv_ruleIssueKindElement= ruleIssueKindElement EOF ;
    public final EObject entryRuleIssueKindElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueKindElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:895:2: (iv_ruleIssueKindElement= ruleIssueKindElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:896:2: iv_ruleIssueKindElement= ruleIssueKindElement EOF
            {
             newCompositeNode(grammarAccess.getIssueKindElementRule()); 
            pushFollow(FOLLOW_ruleIssueKindElement_in_entryRuleIssueKindElement1896);
            iv_ruleIssueKindElement=ruleIssueKindElement();

            state._fsp--;

             current =iv_ruleIssueKindElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueKindElement1906); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueKindElement"


    // $ANTLR start "ruleIssueKindElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:903:1: ruleIssueKindElement returns [EObject current=null] : ( ( (lv_name_0_0= 'Kinds:' ) ) ( (lv_elements_1_0= ruleIssueKinds ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) ) )* ) ;
    public final EObject ruleIssueKindElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        Token otherlv_2=null;
        AntlrDatatypeRuleToken lv_elements_1_0 = null;

        AntlrDatatypeRuleToken lv_elements_3_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:906:28: ( ( ( (lv_name_0_0= 'Kinds:' ) ) ( (lv_elements_1_0= ruleIssueKinds ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) ) )* ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:907:1: ( ( (lv_name_0_0= 'Kinds:' ) ) ( (lv_elements_1_0= ruleIssueKinds ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) ) )* )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:907:1: ( ( (lv_name_0_0= 'Kinds:' ) ) ( (lv_elements_1_0= ruleIssueKinds ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) ) )* )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:907:2: ( (lv_name_0_0= 'Kinds:' ) ) ( (lv_elements_1_0= ruleIssueKinds ) ) (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) ) )*
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:907:2: ( (lv_name_0_0= 'Kinds:' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:908:1: (lv_name_0_0= 'Kinds:' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:908:1: (lv_name_0_0= 'Kinds:' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:909:3: lv_name_0_0= 'Kinds:'
            {
            lv_name_0_0=(Token)match(input,26,FOLLOW_26_in_ruleIssueKindElement1949); 

                    newLeafNode(lv_name_0_0, grammarAccess.getIssueKindElementAccess().getNameKindsKeyword_0_0());
                

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssueKindElementRule());
            	        }
                   		setWithLastConsumed(current, "name", lv_name_0_0, "Kinds:");
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:922:2: ( (lv_elements_1_0= ruleIssueKinds ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:923:1: (lv_elements_1_0= ruleIssueKinds )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:923:1: (lv_elements_1_0= ruleIssueKinds )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:924:3: lv_elements_1_0= ruleIssueKinds
            {
             
            	        newCompositeNode(grammarAccess.getIssueKindElementAccess().getElementsIssueKindsParserRuleCall_1_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueKinds_in_ruleIssueKindElement1983);
            lv_elements_1_0=ruleIssueKinds();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueKindElementRule());
            	        }
                   		add(
                   			current, 
                   			"elements",
                    		lv_elements_1_0, 
                    		"IssueKinds");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:940:2: (otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) ) )*
            loop11:
            do {
                int alt11=2;
                int LA11_0 = input.LA(1);

                if ( (LA11_0==15) ) {
                    alt11=1;
                }


                switch (alt11) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:940:4: otherlv_2= ',' ( (lv_elements_3_0= ruleIssueKinds ) )
            	    {
            	    otherlv_2=(Token)match(input,15,FOLLOW_15_in_ruleIssueKindElement1996); 

            	        	newLeafNode(otherlv_2, grammarAccess.getIssueKindElementAccess().getCommaKeyword_2_0());
            	        
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:944:1: ( (lv_elements_3_0= ruleIssueKinds ) )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:945:1: (lv_elements_3_0= ruleIssueKinds )
            	    {
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:945:1: (lv_elements_3_0= ruleIssueKinds )
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:946:3: lv_elements_3_0= ruleIssueKinds
            	    {
            	     
            	    	        newCompositeNode(grammarAccess.getIssueKindElementAccess().getElementsIssueKindsParserRuleCall_2_1_0()); 
            	    	    
            	    pushFollow(FOLLOW_ruleIssueKinds_in_ruleIssueKindElement2017);
            	    lv_elements_3_0=ruleIssueKinds();

            	    state._fsp--;


            	    	        if (current==null) {
            	    	            current = createModelElementForParent(grammarAccess.getIssueKindElementRule());
            	    	        }
            	           		add(
            	           			current, 
            	           			"elements",
            	            		lv_elements_3_0, 
            	            		"IssueKinds");
            	    	        afterParserOrEnumRuleCall();
            	    	    

            	    }


            	    }


            	    }
            	    break;

            	default :
            	    break loop11;
                }
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueKindElement"


    // $ANTLR start "entryRuleIssueRelevanceElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:970:1: entryRuleIssueRelevanceElement returns [EObject current=null] : iv_ruleIssueRelevanceElement= ruleIssueRelevanceElement EOF ;
    public final EObject entryRuleIssueRelevanceElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueRelevanceElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:971:2: (iv_ruleIssueRelevanceElement= ruleIssueRelevanceElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:972:2: iv_ruleIssueRelevanceElement= ruleIssueRelevanceElement EOF
            {
             newCompositeNode(grammarAccess.getIssueRelevanceElementRule()); 
            pushFollow(FOLLOW_ruleIssueRelevanceElement_in_entryRuleIssueRelevanceElement2055);
            iv_ruleIssueRelevanceElement=ruleIssueRelevanceElement();

            state._fsp--;

             current =iv_ruleIssueRelevanceElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueRelevanceElement2065); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueRelevanceElement"


    // $ANTLR start "ruleIssueRelevanceElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:979:1: ruleIssueRelevanceElement returns [EObject current=null] : ( ( (lv_name_0_0= 'Relevance:' ) ) ( (lv_relevance_1_0= RULE_INT ) ) ) ;
    public final EObject ruleIssueRelevanceElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        Token lv_relevance_1_0=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:982:28: ( ( ( (lv_name_0_0= 'Relevance:' ) ) ( (lv_relevance_1_0= RULE_INT ) ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:983:1: ( ( (lv_name_0_0= 'Relevance:' ) ) ( (lv_relevance_1_0= RULE_INT ) ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:983:1: ( ( (lv_name_0_0= 'Relevance:' ) ) ( (lv_relevance_1_0= RULE_INT ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:983:2: ( (lv_name_0_0= 'Relevance:' ) ) ( (lv_relevance_1_0= RULE_INT ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:983:2: ( (lv_name_0_0= 'Relevance:' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:984:1: (lv_name_0_0= 'Relevance:' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:984:1: (lv_name_0_0= 'Relevance:' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:985:3: lv_name_0_0= 'Relevance:'
            {
            lv_name_0_0=(Token)match(input,27,FOLLOW_27_in_ruleIssueRelevanceElement2108); 

                    newLeafNode(lv_name_0_0, grammarAccess.getIssueRelevanceElementAccess().getNameRelevanceKeyword_0_0());
                

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssueRelevanceElementRule());
            	        }
                   		setWithLastConsumed(current, "name", lv_name_0_0, "Relevance:");
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:998:2: ( (lv_relevance_1_0= RULE_INT ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:999:1: (lv_relevance_1_0= RULE_INT )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:999:1: (lv_relevance_1_0= RULE_INT )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1000:3: lv_relevance_1_0= RULE_INT
            {
            lv_relevance_1_0=(Token)match(input,RULE_INT,FOLLOW_RULE_INT_in_ruleIssueRelevanceElement2138); 

            			newLeafNode(lv_relevance_1_0, grammarAccess.getIssueRelevanceElementAccess().getRelevanceINTTerminalRuleCall_1_0()); 
            		

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssueRelevanceElementRule());
            	        }
                   		setWithLastConsumed(
                   			current, 
                   			"relevance",
                    		lv_relevance_1_0, 
                    		"INT");
            	    

            }


            }


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueRelevanceElement"


    // $ANTLR start "entryRuleIssuePackageElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1024:1: entryRuleIssuePackageElement returns [EObject current=null] : iv_ruleIssuePackageElement= ruleIssuePackageElement EOF ;
    public final EObject entryRuleIssuePackageElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssuePackageElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1025:2: (iv_ruleIssuePackageElement= ruleIssuePackageElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1026:2: iv_ruleIssuePackageElement= ruleIssuePackageElement EOF
            {
             newCompositeNode(grammarAccess.getIssuePackageElementRule()); 
            pushFollow(FOLLOW_ruleIssuePackageElement_in_entryRuleIssuePackageElement2179);
            iv_ruleIssuePackageElement=ruleIssuePackageElement();

            state._fsp--;

             current =iv_ruleIssuePackageElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssuePackageElement2189); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssuePackageElement"


    // $ANTLR start "ruleIssuePackageElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1033:1: ruleIssuePackageElement returns [EObject current=null] : ( ( (lv_name_0_0= 'Package:' ) ) ( (lv_package_1_0= ruleSlashPath ) ) ) ;
    public final EObject ruleIssuePackageElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        AntlrDatatypeRuleToken lv_package_1_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1036:28: ( ( ( (lv_name_0_0= 'Package:' ) ) ( (lv_package_1_0= ruleSlashPath ) ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1037:1: ( ( (lv_name_0_0= 'Package:' ) ) ( (lv_package_1_0= ruleSlashPath ) ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1037:1: ( ( (lv_name_0_0= 'Package:' ) ) ( (lv_package_1_0= ruleSlashPath ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1037:2: ( (lv_name_0_0= 'Package:' ) ) ( (lv_package_1_0= ruleSlashPath ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1037:2: ( (lv_name_0_0= 'Package:' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1038:1: (lv_name_0_0= 'Package:' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1038:1: (lv_name_0_0= 'Package:' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1039:3: lv_name_0_0= 'Package:'
            {
            lv_name_0_0=(Token)match(input,28,FOLLOW_28_in_ruleIssuePackageElement2232); 

                    newLeafNode(lv_name_0_0, grammarAccess.getIssuePackageElementAccess().getNamePackageKeyword_0_0());
                

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssuePackageElementRule());
            	        }
                   		setWithLastConsumed(current, "name", lv_name_0_0, "Package:");
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1052:2: ( (lv_package_1_0= ruleSlashPath ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1053:1: (lv_package_1_0= ruleSlashPath )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1053:1: (lv_package_1_0= ruleSlashPath )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1054:3: lv_package_1_0= ruleSlashPath
            {
             
            	        newCompositeNode(grammarAccess.getIssuePackageElementAccess().getPackageSlashPathParserRuleCall_1_0()); 
            	    
            pushFollow(FOLLOW_ruleSlashPath_in_ruleIssuePackageElement2266);
            lv_package_1_0=ruleSlashPath();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssuePackageElementRule());
            	        }
                   		set(
                   			current, 
                   			"package",
                    		lv_package_1_0, 
                    		"SlashPath");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssuePackageElement"


    // $ANTLR start "entryRuleIssueClassElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1078:1: entryRuleIssueClassElement returns [EObject current=null] : iv_ruleIssueClassElement= ruleIssueClassElement EOF ;
    public final EObject entryRuleIssueClassElement() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueClassElement = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1079:2: (iv_ruleIssueClassElement= ruleIssueClassElement EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1080:2: iv_ruleIssueClassElement= ruleIssueClassElement EOF
            {
             newCompositeNode(grammarAccess.getIssueClassElementRule()); 
            pushFollow(FOLLOW_ruleIssueClassElement_in_entryRuleIssueClassElement2302);
            iv_ruleIssueClassElement=ruleIssueClassElement();

            state._fsp--;

             current =iv_ruleIssueClassElement; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueClassElement2312); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueClassElement"


    // $ANTLR start "ruleIssueClassElement"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1087:1: ruleIssueClassElement returns [EObject current=null] : ( ( (lv_name_0_0= 'Class:' ) ) ( (lv_class_1_0= ruleIssueClass ) ) ) ;
    public final EObject ruleIssueClassElement() throws RecognitionException {
        EObject current = null;

        Token lv_name_0_0=null;
        AntlrDatatypeRuleToken lv_class_1_0 = null;


         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1090:28: ( ( ( (lv_name_0_0= 'Class:' ) ) ( (lv_class_1_0= ruleIssueClass ) ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1091:1: ( ( (lv_name_0_0= 'Class:' ) ) ( (lv_class_1_0= ruleIssueClass ) ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1091:1: ( ( (lv_name_0_0= 'Class:' ) ) ( (lv_class_1_0= ruleIssueClass ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1091:2: ( (lv_name_0_0= 'Class:' ) ) ( (lv_class_1_0= ruleIssueClass ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1091:2: ( (lv_name_0_0= 'Class:' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1092:1: (lv_name_0_0= 'Class:' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1092:1: (lv_name_0_0= 'Class:' )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1093:3: lv_name_0_0= 'Class:'
            {
            lv_name_0_0=(Token)match(input,29,FOLLOW_29_in_ruleIssueClassElement2355); 

                    newLeafNode(lv_name_0_0, grammarAccess.getIssueClassElementAccess().getNameClassKeyword_0_0());
                

            	        if (current==null) {
            	            current = createModelElement(grammarAccess.getIssueClassElementRule());
            	        }
                   		setWithLastConsumed(current, "name", lv_name_0_0, "Class:");
            	    

            }


            }

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1106:2: ( (lv_class_1_0= ruleIssueClass ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1107:1: (lv_class_1_0= ruleIssueClass )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1107:1: (lv_class_1_0= ruleIssueClass )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1108:3: lv_class_1_0= ruleIssueClass
            {
             
            	        newCompositeNode(grammarAccess.getIssueClassElementAccess().getClassIssueClassParserRuleCall_1_0()); 
            	    
            pushFollow(FOLLOW_ruleIssueClass_in_ruleIssueClassElement2389);
            lv_class_1_0=ruleIssueClass();

            state._fsp--;


            	        if (current==null) {
            	            current = createModelElementForParent(grammarAccess.getIssueClassElementRule());
            	        }
                   		set(
                   			current, 
                   			"class",
                    		lv_class_1_0, 
                    		"IssueClass");
            	        afterParserOrEnumRuleCall();
            	    

            }


            }


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueClassElement"


    // $ANTLR start "entryRuleIssueTypes"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1132:1: entryRuleIssueTypes returns [String current=null] : iv_ruleIssueTypes= ruleIssueTypes EOF ;
    public final String entryRuleIssueTypes() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleIssueTypes = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1133:2: (iv_ruleIssueTypes= ruleIssueTypes EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1134:2: iv_ruleIssueTypes= ruleIssueTypes EOF
            {
             newCompositeNode(grammarAccess.getIssueTypesRule()); 
            pushFollow(FOLLOW_ruleIssueTypes_in_entryRuleIssueTypes2426);
            iv_ruleIssueTypes=ruleIssueTypes();

            state._fsp--;

             current =iv_ruleIssueTypes.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueTypes2437); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueTypes"


    // $ANTLR start "ruleIssueTypes"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1141:1: ruleIssueTypes returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : (kw= 'DeadEdgesAnalysis' | kw= 'UnusedLocalVariables' | kw= 'GuardedAndUnguardedAccessAnalysis' | kw= 'UnusedMethodsAnalysis' | kw= 'UselessComputationsAnalysis' ) ;
    public final AntlrDatatypeRuleToken ruleIssueTypes() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token kw=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1144:28: ( (kw= 'DeadEdgesAnalysis' | kw= 'UnusedLocalVariables' | kw= 'GuardedAndUnguardedAccessAnalysis' | kw= 'UnusedMethodsAnalysis' | kw= 'UselessComputationsAnalysis' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1145:1: (kw= 'DeadEdgesAnalysis' | kw= 'UnusedLocalVariables' | kw= 'GuardedAndUnguardedAccessAnalysis' | kw= 'UnusedMethodsAnalysis' | kw= 'UselessComputationsAnalysis' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1145:1: (kw= 'DeadEdgesAnalysis' | kw= 'UnusedLocalVariables' | kw= 'GuardedAndUnguardedAccessAnalysis' | kw= 'UnusedMethodsAnalysis' | kw= 'UselessComputationsAnalysis' )
            int alt12=5;
            switch ( input.LA(1) ) {
            case 30:
                {
                alt12=1;
                }
                break;
            case 31:
                {
                alt12=2;
                }
                break;
            case 32:
                {
                alt12=3;
                }
                break;
            case 33:
                {
                alt12=4;
                }
                break;
            case 34:
                {
                alt12=5;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 12, 0, input);

                throw nvae;
            }

            switch (alt12) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1146:2: kw= 'DeadEdgesAnalysis'
                    {
                    kw=(Token)match(input,30,FOLLOW_30_in_ruleIssueTypes2475); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueTypesAccess().getDeadEdgesAnalysisKeyword_0()); 
                        

                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1153:2: kw= 'UnusedLocalVariables'
                    {
                    kw=(Token)match(input,31,FOLLOW_31_in_ruleIssueTypes2494); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueTypesAccess().getUnusedLocalVariablesKeyword_1()); 
                        

                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1160:2: kw= 'GuardedAndUnguardedAccessAnalysis'
                    {
                    kw=(Token)match(input,32,FOLLOW_32_in_ruleIssueTypes2513); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueTypesAccess().getGuardedAndUnguardedAccessAnalysisKeyword_2()); 
                        

                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1167:2: kw= 'UnusedMethodsAnalysis'
                    {
                    kw=(Token)match(input,33,FOLLOW_33_in_ruleIssueTypes2532); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueTypesAccess().getUnusedMethodsAnalysisKeyword_3()); 
                        

                    }
                    break;
                case 5 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1174:2: kw= 'UselessComputationsAnalysis'
                    {
                    kw=(Token)match(input,34,FOLLOW_34_in_ruleIssueTypes2551); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueTypesAccess().getUselessComputationsAnalysisKeyword_4()); 
                        

                    }
                    break;

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueTypes"


    // $ANTLR start "entryRuleIssueCategories"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1187:1: entryRuleIssueCategories returns [EObject current=null] : iv_ruleIssueCategories= ruleIssueCategories EOF ;
    public final EObject entryRuleIssueCategories() throws RecognitionException {
        EObject current = null;

        EObject iv_ruleIssueCategories = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1188:2: (iv_ruleIssueCategories= ruleIssueCategories EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1189:2: iv_ruleIssueCategories= ruleIssueCategories EOF
            {
             newCompositeNode(grammarAccess.getIssueCategoriesRule()); 
            pushFollow(FOLLOW_ruleIssueCategories_in_entryRuleIssueCategories2591);
            iv_ruleIssueCategories=ruleIssueCategories();

            state._fsp--;

             current =iv_ruleIssueCategories; 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueCategories2601); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueCategories"


    // $ANTLR start "ruleIssueCategories"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1196:1: ruleIssueCategories returns [EObject current=null] : ( ( (lv_bug_0_0= 'bug' ) ) | ( (lv_smell_1_0= 'smell' ) ) | ( (lv_performance_2_0= 'performance' ) ) | ( (lv_comprehensibility_3_0= 'comprehensibility' ) ) ) ;
    public final EObject ruleIssueCategories() throws RecognitionException {
        EObject current = null;

        Token lv_bug_0_0=null;
        Token lv_smell_1_0=null;
        Token lv_performance_2_0=null;
        Token lv_comprehensibility_3_0=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1199:28: ( ( ( (lv_bug_0_0= 'bug' ) ) | ( (lv_smell_1_0= 'smell' ) ) | ( (lv_performance_2_0= 'performance' ) ) | ( (lv_comprehensibility_3_0= 'comprehensibility' ) ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1200:1: ( ( (lv_bug_0_0= 'bug' ) ) | ( (lv_smell_1_0= 'smell' ) ) | ( (lv_performance_2_0= 'performance' ) ) | ( (lv_comprehensibility_3_0= 'comprehensibility' ) ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1200:1: ( ( (lv_bug_0_0= 'bug' ) ) | ( (lv_smell_1_0= 'smell' ) ) | ( (lv_performance_2_0= 'performance' ) ) | ( (lv_comprehensibility_3_0= 'comprehensibility' ) ) )
            int alt13=4;
            switch ( input.LA(1) ) {
            case 35:
                {
                alt13=1;
                }
                break;
            case 36:
                {
                alt13=2;
                }
                break;
            case 37:
                {
                alt13=3;
                }
                break;
            case 38:
                {
                alt13=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;
            }

            switch (alt13) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1200:2: ( (lv_bug_0_0= 'bug' ) )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1200:2: ( (lv_bug_0_0= 'bug' ) )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1201:1: (lv_bug_0_0= 'bug' )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1201:1: (lv_bug_0_0= 'bug' )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1202:3: lv_bug_0_0= 'bug'
                    {
                    lv_bug_0_0=(Token)match(input,35,FOLLOW_35_in_ruleIssueCategories2644); 

                            newLeafNode(lv_bug_0_0, grammarAccess.getIssueCategoriesAccess().getBugBugKeyword_0_0());
                        

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueCategoriesRule());
                    	        }
                           		setWithLastConsumed(current, "bug", lv_bug_0_0, "bug");
                    	    

                    }


                    }


                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1216:6: ( (lv_smell_1_0= 'smell' ) )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1216:6: ( (lv_smell_1_0= 'smell' ) )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1217:1: (lv_smell_1_0= 'smell' )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1217:1: (lv_smell_1_0= 'smell' )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1218:3: lv_smell_1_0= 'smell'
                    {
                    lv_smell_1_0=(Token)match(input,36,FOLLOW_36_in_ruleIssueCategories2681); 

                            newLeafNode(lv_smell_1_0, grammarAccess.getIssueCategoriesAccess().getSmellSmellKeyword_1_0());
                        

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueCategoriesRule());
                    	        }
                           		setWithLastConsumed(current, "smell", lv_smell_1_0, "smell");
                    	    

                    }


                    }


                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1232:6: ( (lv_performance_2_0= 'performance' ) )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1232:6: ( (lv_performance_2_0= 'performance' ) )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1233:1: (lv_performance_2_0= 'performance' )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1233:1: (lv_performance_2_0= 'performance' )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1234:3: lv_performance_2_0= 'performance'
                    {
                    lv_performance_2_0=(Token)match(input,37,FOLLOW_37_in_ruleIssueCategories2718); 

                            newLeafNode(lv_performance_2_0, grammarAccess.getIssueCategoriesAccess().getPerformancePerformanceKeyword_2_0());
                        

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueCategoriesRule());
                    	        }
                           		setWithLastConsumed(current, "performance", lv_performance_2_0, "performance");
                    	    

                    }


                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1248:6: ( (lv_comprehensibility_3_0= 'comprehensibility' ) )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1248:6: ( (lv_comprehensibility_3_0= 'comprehensibility' ) )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1249:1: (lv_comprehensibility_3_0= 'comprehensibility' )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1249:1: (lv_comprehensibility_3_0= 'comprehensibility' )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1250:3: lv_comprehensibility_3_0= 'comprehensibility'
                    {
                    lv_comprehensibility_3_0=(Token)match(input,38,FOLLOW_38_in_ruleIssueCategories2755); 

                            newLeafNode(lv_comprehensibility_3_0, grammarAccess.getIssueCategoriesAccess().getComprehensibilityComprehensibilityKeyword_3_0());
                        

                    	        if (current==null) {
                    	            current = createModelElement(grammarAccess.getIssueCategoriesRule());
                    	        }
                           		setWithLastConsumed(current, "comprehensibility", lv_comprehensibility_3_0, "comprehensibility");
                    	    

                    }


                    }


                    }
                    break;

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueCategories"


    // $ANTLR start "entryRuleIssueKinds"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1271:1: entryRuleIssueKinds returns [String current=null] : iv_ruleIssueKinds= ruleIssueKinds EOF ;
    public final String entryRuleIssueKinds() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleIssueKinds = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1272:2: (iv_ruleIssueKinds= ruleIssueKinds EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1273:2: iv_ruleIssueKinds= ruleIssueKinds EOF
            {
             newCompositeNode(grammarAccess.getIssueKindsRule()); 
            pushFollow(FOLLOW_ruleIssueKinds_in_entryRuleIssueKinds2805);
            iv_ruleIssueKinds=ruleIssueKinds();

            state._fsp--;

             current =iv_ruleIssueKinds.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueKinds2816); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueKinds"


    // $ANTLR start "ruleIssueKinds"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1280:1: ruleIssueKinds returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : (kw= 'constant computation' | kw= 'dead path' | kw= 'throws exception' | kw= 'unguarded use' | kw= 'unused' | kw= 'useless' ) ;
    public final AntlrDatatypeRuleToken ruleIssueKinds() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token kw=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1283:28: ( (kw= 'constant computation' | kw= 'dead path' | kw= 'throws exception' | kw= 'unguarded use' | kw= 'unused' | kw= 'useless' ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1284:1: (kw= 'constant computation' | kw= 'dead path' | kw= 'throws exception' | kw= 'unguarded use' | kw= 'unused' | kw= 'useless' )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1284:1: (kw= 'constant computation' | kw= 'dead path' | kw= 'throws exception' | kw= 'unguarded use' | kw= 'unused' | kw= 'useless' )
            int alt14=6;
            switch ( input.LA(1) ) {
            case 39:
                {
                alt14=1;
                }
                break;
            case 40:
                {
                alt14=2;
                }
                break;
            case 41:
                {
                alt14=3;
                }
                break;
            case 42:
                {
                alt14=4;
                }
                break;
            case 43:
                {
                alt14=5;
                }
                break;
            case 44:
                {
                alt14=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 14, 0, input);

                throw nvae;
            }

            switch (alt14) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1285:2: kw= 'constant computation'
                    {
                    kw=(Token)match(input,39,FOLLOW_39_in_ruleIssueKinds2854); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueKindsAccess().getConstantComputationKeyword_0()); 
                        

                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1292:2: kw= 'dead path'
                    {
                    kw=(Token)match(input,40,FOLLOW_40_in_ruleIssueKinds2873); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueKindsAccess().getDeadPathKeyword_1()); 
                        

                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1299:2: kw= 'throws exception'
                    {
                    kw=(Token)match(input,41,FOLLOW_41_in_ruleIssueKinds2892); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueKindsAccess().getThrowsExceptionKeyword_2()); 
                        

                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1306:2: kw= 'unguarded use'
                    {
                    kw=(Token)match(input,42,FOLLOW_42_in_ruleIssueKinds2911); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueKindsAccess().getUnguardedUseKeyword_3()); 
                        

                    }
                    break;
                case 5 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1313:2: kw= 'unused'
                    {
                    kw=(Token)match(input,43,FOLLOW_43_in_ruleIssueKinds2930); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueKindsAccess().getUnusedKeyword_4()); 
                        

                    }
                    break;
                case 6 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1320:2: kw= 'useless'
                    {
                    kw=(Token)match(input,44,FOLLOW_44_in_ruleIssueKinds2949); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getIssueKindsAccess().getUselessKeyword_5()); 
                        

                    }
                    break;

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueKinds"


    // $ANTLR start "entryRuleIssueClass"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1333:1: entryRuleIssueClass returns [String current=null] : iv_ruleIssueClass= ruleIssueClass EOF ;
    public final String entryRuleIssueClass() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleIssueClass = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1334:2: (iv_ruleIssueClass= ruleIssueClass EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1335:2: iv_ruleIssueClass= ruleIssueClass EOF
            {
             newCompositeNode(grammarAccess.getIssueClassRule()); 
            pushFollow(FOLLOW_ruleIssueClass_in_entryRuleIssueClass2990);
            iv_ruleIssueClass=ruleIssueClass();

            state._fsp--;

             current =iv_ruleIssueClass.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleIssueClass3001); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleIssueClass"


    // $ANTLR start "ruleIssueClass"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1342:1: ruleIssueClass returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : (this_ID_0= RULE_ID (kw= '$' this_ID_2= RULE_ID )* (kw= '$' this_INT_4= RULE_INT )* ) ;
    public final AntlrDatatypeRuleToken ruleIssueClass() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token this_ID_0=null;
        Token kw=null;
        Token this_ID_2=null;
        Token this_INT_4=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1345:28: ( (this_ID_0= RULE_ID (kw= '$' this_ID_2= RULE_ID )* (kw= '$' this_INT_4= RULE_INT )* ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1346:1: (this_ID_0= RULE_ID (kw= '$' this_ID_2= RULE_ID )* (kw= '$' this_INT_4= RULE_INT )* )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1346:1: (this_ID_0= RULE_ID (kw= '$' this_ID_2= RULE_ID )* (kw= '$' this_INT_4= RULE_INT )* )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1346:6: this_ID_0= RULE_ID (kw= '$' this_ID_2= RULE_ID )* (kw= '$' this_INT_4= RULE_INT )*
            {
            this_ID_0=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleIssueClass3041); 

            		current.merge(this_ID_0);
                
             
                newLeafNode(this_ID_0, grammarAccess.getIssueClassAccess().getIDTerminalRuleCall_0()); 
                
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1353:1: (kw= '$' this_ID_2= RULE_ID )*
            loop15:
            do {
                int alt15=2;
                int LA15_0 = input.LA(1);

                if ( (LA15_0==45) ) {
                    int LA15_1 = input.LA(2);

                    if ( (LA15_1==RULE_ID) ) {
                        alt15=1;
                    }


                }


                switch (alt15) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1354:2: kw= '$' this_ID_2= RULE_ID
            	    {
            	    kw=(Token)match(input,45,FOLLOW_45_in_ruleIssueClass3060); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getIssueClassAccess().getDollarSignKeyword_1_0()); 
            	        
            	    this_ID_2=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleIssueClass3075); 

            	    		current.merge(this_ID_2);
            	        
            	     
            	        newLeafNode(this_ID_2, grammarAccess.getIssueClassAccess().getIDTerminalRuleCall_1_1()); 
            	        

            	    }
            	    break;

            	default :
            	    break loop15;
                }
            } while (true);

            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1366:3: (kw= '$' this_INT_4= RULE_INT )*
            loop16:
            do {
                int alt16=2;
                int LA16_0 = input.LA(1);

                if ( (LA16_0==45) ) {
                    alt16=1;
                }


                switch (alt16) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1367:2: kw= '$' this_INT_4= RULE_INT
            	    {
            	    kw=(Token)match(input,45,FOLLOW_45_in_ruleIssueClass3096); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getIssueClassAccess().getDollarSignKeyword_2_0()); 
            	        
            	    this_INT_4=(Token)match(input,RULE_INT,FOLLOW_RULE_INT_in_ruleIssueClass3111); 

            	    		current.merge(this_INT_4);
            	        
            	     
            	        newLeafNode(this_INT_4, grammarAccess.getIssueClassAccess().getINTTerminalRuleCall_2_1()); 
            	        

            	    }
            	    break;

            	default :
            	    break loop16;
                }
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleIssueClass"


    // $ANTLR start "entryRuleAnyValues"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1387:1: entryRuleAnyValues returns [String current=null] : iv_ruleAnyValues= ruleAnyValues EOF ;
    public final String entryRuleAnyValues() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleAnyValues = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1388:2: (iv_ruleAnyValues= ruleAnyValues EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1389:2: iv_ruleAnyValues= ruleAnyValues EOF
            {
             newCompositeNode(grammarAccess.getAnyValuesRule()); 
            pushFollow(FOLLOW_ruleAnyValues_in_entryRuleAnyValues3159);
            iv_ruleAnyValues=ruleAnyValues();

            state._fsp--;

             current =iv_ruleAnyValues.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleAnyValues3170); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleAnyValues"


    // $ANTLR start "ruleAnyValues"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1396:1: ruleAnyValues returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : (this_INT_0= RULE_INT | this_ID_1= RULE_ID | (this_INT_2= RULE_INT this_ID_3= RULE_ID ) | (this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT ) ) ;
    public final AntlrDatatypeRuleToken ruleAnyValues() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token this_INT_0=null;
        Token this_ID_1=null;
        Token this_INT_2=null;
        Token this_ID_3=null;
        Token this_INT_4=null;
        Token kw=null;
        Token this_INT_6=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1399:28: ( (this_INT_0= RULE_INT | this_ID_1= RULE_ID | (this_INT_2= RULE_INT this_ID_3= RULE_ID ) | (this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT ) ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1400:1: (this_INT_0= RULE_INT | this_ID_1= RULE_ID | (this_INT_2= RULE_INT this_ID_3= RULE_ID ) | (this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT ) )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1400:1: (this_INT_0= RULE_INT | this_ID_1= RULE_ID | (this_INT_2= RULE_INT this_ID_3= RULE_ID ) | (this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT ) )
            int alt17=4;
            int LA17_0 = input.LA(1);

            if ( (LA17_0==RULE_INT) ) {
                switch ( input.LA(2) ) {
                case 16:
                    {
                    alt17=4;
                    }
                    break;
                case RULE_ID:
                    {
                    int LA17_4 = input.LA(3);

                    if ( (LA17_4==EOF||LA17_4==RULE_ID||LA17_4==20) ) {
                        alt17=3;
                    }
                    else if ( (LA17_4==12||(LA17_4>=18 && LA17_4<=19)) ) {
                        alt17=1;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 17, 4, input);

                        throw nvae;
                    }
                    }
                    break;
                case EOF:
                case 20:
                    {
                    alt17=1;
                    }
                    break;
                default:
                    NoViableAltException nvae =
                        new NoViableAltException("", 17, 1, input);

                    throw nvae;
                }

            }
            else if ( (LA17_0==RULE_ID) ) {
                alt17=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 17, 0, input);

                throw nvae;
            }
            switch (alt17) {
                case 1 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1400:6: this_INT_0= RULE_INT
                    {
                    this_INT_0=(Token)match(input,RULE_INT,FOLLOW_RULE_INT_in_ruleAnyValues3210); 

                    		current.merge(this_INT_0);
                        
                     
                        newLeafNode(this_INT_0, grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_0()); 
                        

                    }
                    break;
                case 2 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1408:10: this_ID_1= RULE_ID
                    {
                    this_ID_1=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleAnyValues3236); 

                    		current.merge(this_ID_1);
                        
                     
                        newLeafNode(this_ID_1, grammarAccess.getAnyValuesAccess().getIDTerminalRuleCall_1()); 
                        

                    }
                    break;
                case 3 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1416:6: (this_INT_2= RULE_INT this_ID_3= RULE_ID )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1416:6: (this_INT_2= RULE_INT this_ID_3= RULE_ID )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1416:11: this_INT_2= RULE_INT this_ID_3= RULE_ID
                    {
                    this_INT_2=(Token)match(input,RULE_INT,FOLLOW_RULE_INT_in_ruleAnyValues3263); 

                    		current.merge(this_INT_2);
                        
                     
                        newLeafNode(this_INT_2, grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_2_0()); 
                        
                    this_ID_3=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleAnyValues3283); 

                    		current.merge(this_ID_3);
                        
                     
                        newLeafNode(this_ID_3, grammarAccess.getAnyValuesAccess().getIDTerminalRuleCall_2_1()); 
                        

                    }


                    }
                    break;
                case 4 :
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1431:6: (this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT )
                    {
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1431:6: (this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT )
                    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1431:11: this_INT_4= RULE_INT kw= '.' this_INT_6= RULE_INT
                    {
                    this_INT_4=(Token)match(input,RULE_INT,FOLLOW_RULE_INT_in_ruleAnyValues3311); 

                    		current.merge(this_INT_4);
                        
                     
                        newLeafNode(this_INT_4, grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_3_0()); 
                        
                    kw=(Token)match(input,16,FOLLOW_16_in_ruleAnyValues3329); 

                            current.merge(kw);
                            newLeafNode(kw, grammarAccess.getAnyValuesAccess().getFullStopKeyword_3_1()); 
                        
                    this_INT_6=(Token)match(input,RULE_INT,FOLLOW_RULE_INT_in_ruleAnyValues3344); 

                    		current.merge(this_INT_6);
                        
                     
                        newLeafNode(this_INT_6, grammarAccess.getAnyValuesAccess().getINTTerminalRuleCall_3_2()); 
                        

                    }


                    }
                    break;

            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleAnyValues"


    // $ANTLR start "entryRuleSlashPath"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1463:1: entryRuleSlashPath returns [String current=null] : iv_ruleSlashPath= ruleSlashPath EOF ;
    public final String entryRuleSlashPath() throws RecognitionException {
        String current = null;

        AntlrDatatypeRuleToken iv_ruleSlashPath = null;


        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1464:2: (iv_ruleSlashPath= ruleSlashPath EOF )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1465:2: iv_ruleSlashPath= ruleSlashPath EOF
            {
             newCompositeNode(grammarAccess.getSlashPathRule()); 
            pushFollow(FOLLOW_ruleSlashPath_in_entryRuleSlashPath3395);
            iv_ruleSlashPath=ruleSlashPath();

            state._fsp--;

             current =iv_ruleSlashPath.getText(); 
            match(input,EOF,FOLLOW_EOF_in_entryRuleSlashPath3406); 

            }

        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "entryRuleSlashPath"


    // $ANTLR start "ruleSlashPath"
    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1472:1: ruleSlashPath returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] : (this_ID_0= RULE_ID (kw= '/' this_ID_2= RULE_ID )* ) ;
    public final AntlrDatatypeRuleToken ruleSlashPath() throws RecognitionException {
        AntlrDatatypeRuleToken current = new AntlrDatatypeRuleToken();

        Token this_ID_0=null;
        Token kw=null;
        Token this_ID_2=null;

         enterRule(); 
            
        try {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1475:28: ( (this_ID_0= RULE_ID (kw= '/' this_ID_2= RULE_ID )* ) )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1476:1: (this_ID_0= RULE_ID (kw= '/' this_ID_2= RULE_ID )* )
            {
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1476:1: (this_ID_0= RULE_ID (kw= '/' this_ID_2= RULE_ID )* )
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1476:6: this_ID_0= RULE_ID (kw= '/' this_ID_2= RULE_ID )*
            {
            this_ID_0=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleSlashPath3446); 

            		current.merge(this_ID_0);
                
             
                newLeafNode(this_ID_0, grammarAccess.getSlashPathAccess().getIDTerminalRuleCall_0()); 
                
            // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1483:1: (kw= '/' this_ID_2= RULE_ID )*
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);

                if ( (LA18_0==13) ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // ../org.opalj.bdl/src-gen/org/opalj/bdl/parser/antlr/internal/InternalBDL.g:1484:2: kw= '/' this_ID_2= RULE_ID
            	    {
            	    kw=(Token)match(input,13,FOLLOW_13_in_ruleSlashPath3465); 

            	            current.merge(kw);
            	            newLeafNode(kw, grammarAccess.getSlashPathAccess().getSolidusKeyword_1_0()); 
            	        
            	    this_ID_2=(Token)match(input,RULE_ID,FOLLOW_RULE_ID_in_ruleSlashPath3480); 

            	    		current.merge(this_ID_2);
            	        
            	     
            	        newLeafNode(this_ID_2, grammarAccess.getSlashPathAccess().getIDTerminalRuleCall_1_1()); 
            	        

            	    }
            	    break;

            	default :
            	    break loop18;
                }
            } while (true);


            }


            }

             leaveRule(); 
        }
         
            catch (RecognitionException re) { 
                recover(input,re); 
                appendSkippedTokens();
            } 
        finally {
        }
        return current;
    }
    // $ANTLR end "ruleSlashPath"

    // Delegated rules


 

    public static final BitSet FOLLOW_ruleModel_in_entryRuleModel75 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleModel85 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleModelContainer_in_ruleModel131 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleModelContainer_in_entryRuleModelContainer165 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleModelContainer175 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleAnalysisElement_in_ruleModelContainer221 = new BitSet(new long[]{0x0000000000020000L});
    public static final BitSet FOLLOW_ruleParameterContainer_in_ruleModelContainer242 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ruleIssuesContainer_in_ruleModelContainer263 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleAnalysisElement_in_entryRuleAnalysisElement300 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleAnalysisElement311 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_11_in_ruleAnalysisElement349 = new BitSet(new long[]{0x000000000001F010L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleAnalysisElement365 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_12_in_ruleAnalysisElement389 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_13_in_ruleAnalysisElement408 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_14_in_ruleAnalysisElement427 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_15_in_ruleAnalysisElement446 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_16_in_ruleAnalysisElement465 = new BitSet(new long[]{0x000000000001F012L});
    public static final BitSet FOLLOW_ruleParameterContainer_in_entryRuleParameterContainer507 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterContainer517 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParametersElement_in_ruleParameterContainer563 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ruleParameterElement_in_ruleParameterContainer584 = new BitSet(new long[]{0x0000000000000012L});
    public static final BitSet FOLLOW_ruleParametersElement_in_entryRuleParametersElement622 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParametersElement633 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_17_in_ruleParametersElement670 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterElement_in_entryRuleParameterElement709 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterElement719 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyValueElement_in_ruleParameterElement766 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyElement_in_ruleParameterElement793 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyValueElement_in_entryRuleParameterKeyValueElement828 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterKeyValueElement838 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleParameterKeyValueElement880 = new BitSet(new long[]{0x0000000000041000L});
    public static final BitSet FOLLOW_12_in_ruleParameterKeyValueElement898 = new BitSet(new long[]{0x0000000000000050L});
    public static final BitSet FOLLOW_18_in_ruleParameterKeyValueElement916 = new BitSet(new long[]{0x0000000000000050L});
    public static final BitSet FOLLOW_ruleAnyValues_in_ruleParameterKeyValueElement938 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleParameterKeyElement_in_entryRuleParameterKeyElement974 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleParameterKeyElement984 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleParameterKeyElement1026 = new BitSet(new long[]{0x0000000000080000L});
    public static final BitSet FOLLOW_19_in_ruleParameterKeyElement1043 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuesContainer_in_entryRuleIssuesContainer1079 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssuesContainer1089 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuesTitleElement_in_ruleIssuesContainer1136 = new BitSet(new long[]{0x00000007C0000002L});
    public static final BitSet FOLLOW_ruleIssueElement_in_ruleIssuesContainer1156 = new BitSet(new long[]{0x00000007C0000002L});
    public static final BitSet FOLLOW_ruleIssuesTitleElement_in_entryRuleIssuesTitleElement1193 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssuesTitleElement1203 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_20_in_ruleIssuesTitleElement1245 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueElement_in_entryRuleIssueElement1293 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueElement1303 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueTypes_in_ruleIssueElement1349 = new BitSet(new long[]{0x0000000000A08000L});
    public static final BitSet FOLLOW_15_in_ruleIssueElement1362 = new BitSet(new long[]{0x00000007C0000000L});
    public static final BitSet FOLLOW_ruleIssueTypes_in_ruleIssueElement1383 = new BitSet(new long[]{0x0000000000A08000L});
    public static final BitSet FOLLOW_ruleIssueSuppressComment_in_ruleIssueElement1406 = new BitSet(new long[]{0x0000000000200000L});
    public static final BitSet FOLLOW_21_in_ruleIssueElement1419 = new BitSet(new long[]{0x0000000002000020L});
    public static final BitSet FOLLOW_RULE_STRING_in_ruleIssueElement1436 = new BitSet(new long[]{0x0000000002000020L});
    public static final BitSet FOLLOW_ruleIssueCategoryElement_in_ruleIssueElement1463 = new BitSet(new long[]{0x0000000004000000L});
    public static final BitSet FOLLOW_ruleIssueKindElement_in_ruleIssueElement1484 = new BitSet(new long[]{0x0000000008000000L});
    public static final BitSet FOLLOW_ruleIssueRelevanceElement_in_ruleIssueElement1505 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_ruleIssuePackageElement_in_ruleIssueElement1526 = new BitSet(new long[]{0x0000000020000000L});
    public static final BitSet FOLLOW_ruleIssueClassElement_in_ruleIssueElement1547 = new BitSet(new long[]{0x0000000000400000L});
    public static final BitSet FOLLOW_22_in_ruleIssueElement1559 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueSuppressComment_in_entryRuleIssueSuppressComment1595 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueSuppressComment1605 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_23_in_ruleIssueSuppressComment1642 = new BitSet(new long[]{0x0000000000000030L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleIssueSuppressComment1661 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_RULE_STRING_in_ruleIssueSuppressComment1681 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_ruleIssueSuppressComment1701 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategoryElement_in_entryRuleIssueCategoryElement1737 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueCategoryElement1747 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_25_in_ruleIssueCategoryElement1790 = new BitSet(new long[]{0x0000007800000000L});
    public static final BitSet FOLLOW_ruleIssueCategories_in_ruleIssueCategoryElement1824 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_15_in_ruleIssueCategoryElement1837 = new BitSet(new long[]{0x0000007800000000L});
    public static final BitSet FOLLOW_ruleIssueCategories_in_ruleIssueCategoryElement1858 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_ruleIssueKindElement_in_entryRuleIssueKindElement1896 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueKindElement1906 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_26_in_ruleIssueKindElement1949 = new BitSet(new long[]{0x00001F8000000000L});
    public static final BitSet FOLLOW_ruleIssueKinds_in_ruleIssueKindElement1983 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_15_in_ruleIssueKindElement1996 = new BitSet(new long[]{0x00001F8000000000L});
    public static final BitSet FOLLOW_ruleIssueKinds_in_ruleIssueKindElement2017 = new BitSet(new long[]{0x0000000000008002L});
    public static final BitSet FOLLOW_ruleIssueRelevanceElement_in_entryRuleIssueRelevanceElement2055 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueRelevanceElement2065 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_27_in_ruleIssueRelevanceElement2108 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_RULE_INT_in_ruleIssueRelevanceElement2138 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssuePackageElement_in_entryRuleIssuePackageElement2179 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssuePackageElement2189 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_28_in_ruleIssuePackageElement2232 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ruleSlashPath_in_ruleIssuePackageElement2266 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueClassElement_in_entryRuleIssueClassElement2302 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueClassElement2312 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_29_in_ruleIssueClassElement2355 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_ruleIssueClass_in_ruleIssueClassElement2389 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueTypes_in_entryRuleIssueTypes2426 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueTypes2437 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_30_in_ruleIssueTypes2475 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_31_in_ruleIssueTypes2494 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_32_in_ruleIssueTypes2513 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_33_in_ruleIssueTypes2532 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_34_in_ruleIssueTypes2551 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueCategories_in_entryRuleIssueCategories2591 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueCategories2601 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_35_in_ruleIssueCategories2644 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_36_in_ruleIssueCategories2681 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_ruleIssueCategories2718 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_38_in_ruleIssueCategories2755 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueKinds_in_entryRuleIssueKinds2805 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueKinds2816 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_39_in_ruleIssueKinds2854 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_40_in_ruleIssueKinds2873 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_41_in_ruleIssueKinds2892 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_42_in_ruleIssueKinds2911 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_43_in_ruleIssueKinds2930 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_44_in_ruleIssueKinds2949 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleIssueClass_in_entryRuleIssueClass2990 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleIssueClass3001 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleIssueClass3041 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_45_in_ruleIssueClass3060 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleIssueClass3075 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_45_in_ruleIssueClass3096 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_RULE_INT_in_ruleIssueClass3111 = new BitSet(new long[]{0x0000200000000002L});
    public static final BitSet FOLLOW_ruleAnyValues_in_entryRuleAnyValues3159 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleAnyValues3170 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_ruleAnyValues3210 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleAnyValues3236 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_ruleAnyValues3263 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleAnyValues3283 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_INT_in_ruleAnyValues3311 = new BitSet(new long[]{0x0000000000010000L});
    public static final BitSet FOLLOW_16_in_ruleAnyValues3329 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_RULE_INT_in_ruleAnyValues3344 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ruleSlashPath_in_entryRuleSlashPath3395 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_entryRuleSlashPath3406 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleSlashPath3446 = new BitSet(new long[]{0x0000000000002002L});
    public static final BitSet FOLLOW_13_in_ruleSlashPath3465 = new BitSet(new long[]{0x0000000000000010L});
    public static final BitSet FOLLOW_RULE_ID_in_ruleSlashPath3480 = new BitSet(new long[]{0x0000000000002002L});

}