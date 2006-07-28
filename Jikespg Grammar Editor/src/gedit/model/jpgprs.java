package gedit.model;
public class jpgprs implements jpgsym {

    public interface IsKeyword {
        public final static byte isKeyword[] = {0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0
        };
    };
    public final static byte isKeyword[] = IsKeyword.isKeyword;
    public final boolean isKeyword(int index) { return isKeyword[index] != 0; }

    public interface BaseCheck {
        public final static byte baseCheck[] = {0,
            1,1,0,3,3,3,3,3,3,3,
            3,3,3,3,3,3,3,3,3,3,
            3,3,3,3,1,1,1,1,1,1,
            1,1,1,2,3,1,1,2,1,3,
            1,1,1,1,2,4,1,2,1,3,
            1,1,1,1,1,2,1,2,3,1,
            2,3,0,2,1,2,4,1,2,1,
            1,2,3,1,1,2,1,1,2,1,
            1,2,1,1,1,1,4,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            2,1,1,2,1,2,1,1,2,1,
            2,1,1,2,2,1,3,3,1,2,
            3,1,2,1,1,1,1,1,0,2,
            2,1,1,2,3,3,1,1,4,1,
            1,1,1,1,1,0,1,0,2,-3,
            -49,-13,0,0,-22,0,-2,0,-31,0,
            -24,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,-14,0,0,-25,0,0,0,
            0,-28,0,0,0,0,0,-1,0,0,
            -58,0,0,-27,0,0,0,0,0,-15,
            0,0,0,0,0,-16,0,0,0,-18,
            0,-32,0,-26,0,-29,0,0,0,-30,
            0,0,-33,0,0,-48,-60,-61,0,0,
            0,0,0,0,0,-68,-69,0,0,-4,
            0,-35,0,0,-5,0,-6,0,-7,0,
            -8,0,-9,0,-10,0,-11,0,-19,-20,
            -21,0,0,0,-23,0,-36,0,-37,-38,
            0,-40,-51,0,-41,0,0,-43,0,0,
            -45,0,0,-47,-50,-52,-53,0,-54,0,
            0,-56,0,-57,0,-12,-63,-66,-67,0,
            0,0,-17,0,0,0,-34,-39,-42,-44,
            -46,-55,-59,-62,-64,-65,0,0,0,0,
            0,0,0
        };
    };
    public final static byte baseCheck[] = BaseCheck.baseCheck;
    public final int baseCheck(int index) { return baseCheck[index]; }
    public final static byte rhs[] = baseCheck;
    public final int rhs(int index) { return rhs[index]; };

    public interface BaseAction {
        public final static char baseAction[] = {
            9,9,9,10,10,10,10,10,10,10,
            10,10,10,10,10,10,10,10,10,10,
            10,10,10,10,10,11,11,11,11,11,
            11,11,11,12,12,12,33,13,13,14,
            14,34,34,35,15,15,15,16,16,17,
            17,36,38,38,38,39,39,40,40,41,
            42,42,41,37,37,18,18,18,21,21,
            43,22,22,22,44,23,23,45,20,20,
            46,19,19,47,3,3,25,25,48,48,
            48,48,48,48,49,49,49,49,49,49,
            49,24,50,27,27,28,28,52,29,29,
            30,30,51,51,31,31,53,55,55,56,
            56,56,7,7,7,4,4,4,4,6,
            6,6,2,32,32,57,57,8,26,26,
            5,5,5,5,5,5,1,1,54,54,
            277,15,315,5,38,422,14,317,74,281,
            23,189,267,152,184,210,216,265,220,263,
            261,259,155,257,255,187,224,204,250,226,
            230,159,233,306,416,6,313,323,17,64,
            323,201,255,271,106,275,270,269,310,318,
            161,399,192,222,315,19,104,157,150,2,
            422,7,101,277,319,288,422,8,252,48,
            422,10,51,280,334,18,315,21,109,285,
            315,22,111,445,24,282,354,425,430,131,
            131,46,324,307,130,130,435,440,131,131,
            432,20,374,130,130,432,16,432,15,432,
            13,432,12,432,11,432,9,432,4,12,
            84,120,82,79,69,156,291,278,76,75,
            352,236,357,343,295,357,283,40,357,296,
            321,449,299,149,357,354,354,87,304,87,
            67,139,326,237,326,238,222,357,87,87,
            136,309,135,224,246,247,151,256,448,279,
            346,398,373,419,447,453,457,335,335,335,
            335,335,87,335,335
        };
    };
    public final static char baseAction[] = BaseAction.baseAction;
    public final int baseAction(int index) { return baseAction[index]; }
    public final static char lhs[] = baseAction;
    public final int lhs(int index) { return lhs[index]; };

    public interface TermCheck {
        public final static byte termCheck[] = {0,
            0,1,2,3,4,5,6,7,8,9,
            10,0,1,2,0,15,16,17,18,19,
            20,21,22,23,24,25,26,27,28,29,
            30,31,32,33,34,35,0,1,2,3,
            4,5,6,7,8,9,10,33,34,35,
            0,15,16,17,18,19,20,21,22,23,
            24,25,26,27,28,29,30,31,32,33,
            34,35,0,1,0,3,4,5,6,7,
            8,9,10,0,1,2,0,15,16,17,
            18,19,20,21,22,23,24,25,26,27,
            28,29,30,31,32,33,34,35,0,1,
            0,3,4,5,6,7,8,9,10,0,
            1,2,0,15,16,17,18,19,20,21,
            22,23,24,25,26,27,28,29,30,31,
            32,33,34,35,0,1,2,3,4,5,
            6,7,8,9,10,0,1,2,0,15,
            16,17,18,19,20,21,22,23,24,25,
            26,27,28,29,30,31,32,0,1,2,
            3,4,5,6,7,8,9,10,0,1,
            2,0,15,16,17,18,19,20,21,22,
            23,24,25,26,27,28,29,30,31,32,
            0,1,0,3,4,5,6,7,8,9,
            10,0,1,0,1,15,16,17,18,19,
            20,21,22,23,24,25,26,27,28,29,
            30,31,32,0,1,0,3,4,5,6,
            7,8,9,10,0,0,1,3,15,16,
            17,18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,0,0,0,0,
            0,1,6,7,8,5,10,9,11,12,
            13,14,16,17,18,19,20,21,22,23,
            24,25,26,27,28,29,30,31,32,0,
            1,2,3,4,0,6,0,3,9,5,
            11,12,0,1,2,0,1,5,6,7,
            8,15,10,0,1,2,0,4,5,6,
            7,8,0,1,2,0,4,2,6,7,
            8,0,10,0,1,2,0,4,0,6,
            7,8,11,12,13,14,0,11,12,13,
            14,0,0,0,2,0,3,11,12,13,
            14,0,11,12,13,14,11,12,13,14,
            0,0,11,12,13,14,0,0,0,1,
            0,11,12,13,14,0,9,11,12,13,
            14,11,12,13,14,0,1,2,0,1,
            5,0,1,2,0,1,5,3,4,0,
            1,0,3,4,0,1,5,3,4,0,
            1,0,3,4,0,1,0,0,0,5,
            3,3,0,0,2,9,0,0,2,0,
            0,0,0,0,0,0,0,0,15,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0
        };
    };
    public final static byte termCheck[] = TermCheck.termCheck;
    public final int termCheck(int index) { return termCheck[index]; }

    public interface TermAction {
        public final static char termAction[] = {0,
            62,326,557,326,326,326,326,326,326,326,
            326,81,419,420,50,326,326,326,326,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,326,326,326,326,59,326,593,326,
            326,326,326,326,326,326,326,390,389,392,
            148,326,326,326,326,326,326,326,326,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,62,326,63,326,326,326,326,326,
            326,326,326,78,419,420,129,326,326,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,326,326,326,326,326,326,59,326,
            335,326,326,326,326,326,326,326,326,68,
            419,420,335,326,326,326,326,326,326,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,326,326,118,326,695,326,326,326,
            326,326,326,326,326,75,419,420,335,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,326,326,326,326,326,117,326,728,
            326,326,326,326,326,326,326,326,335,447,
            448,335,326,326,326,326,326,326,326,326,
            326,326,326,326,326,326,326,326,326,326,
            118,326,335,326,326,326,326,326,326,326,
            326,33,317,49,279,326,326,326,326,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,326,117,326,335,326,326,326,326,
            326,326,326,326,105,34,370,467,326,326,
            326,326,326,326,326,326,326,326,326,326,
            326,326,326,326,326,326,1,45,115,335,
            146,320,405,415,412,482,418,297,460,462,
            461,463,372,442,443,374,379,400,421,437,
            468,451,473,438,445,382,386,371,406,3,
            366,365,367,363,146,938,335,467,362,482,
            360,361,146,423,428,335,472,482,866,860,
            851,334,856,146,475,476,335,477,482,885,
            881,875,335,429,430,119,434,322,431,432,
            435,66,433,335,475,476,335,477,335,478,
            479,480,460,462,461,463,146,460,462,461,
            463,146,120,335,456,146,378,427,427,427,
            427,146,426,426,426,426,425,425,425,425,
            146,335,424,424,424,424,146,134,53,325,
            146,480,480,480,480,335,302,479,479,479,
            479,478,478,478,478,146,377,376,52,391,
            482,146,419,420,118,629,482,467,459,117,
            662,146,467,459,62,485,482,467,459,59,
            521,335,467,459,146,294,58,72,114,482,
            408,467,60,3,396,308,122,335,458,335,
            335,335,335,335,335,335,335,335,595
        };
    };
    public final static char termAction[] = TermAction.termAction;
    public final int termAction(int index) { return termAction[index]; }

    public interface Asb {
        public final static char asb[] = {0,
            1,179,158,157,157,157,157,157,157,157,
            157,63,118,62,62,62,63,62,62,62,
            62,62,62,28,62,31,118,118,118,118,
            63,117,63,63,141,58,154,58,116,144,
            144,90,144,143,117,90,144,24,154,24,
            23,24,88,88,143,86,86,153,153,88,
            88,152,144,143,149,150,150,150,150
        };
    };
    public final static char asb[] = Asb.asb;
    public final int asb(int index) { return asb[index]; }

    public interface Asr {
        public final static byte asr[] = {0,
            16,17,18,19,20,21,22,23,24,25,
            26,27,28,29,30,31,32,15,11,12,
            9,3,10,4,6,7,8,1,2,0,
            16,17,18,19,20,21,10,22,23,24,
            25,26,27,28,29,30,31,32,15,1,
            2,4,6,7,8,5,0,11,13,12,
            14,2,16,17,18,19,20,21,7,10,
            6,8,22,23,24,25,26,5,27,28,
            29,30,31,32,15,1,0,4,3,16,
            17,18,19,20,21,7,10,6,8,22,
            23,24,25,26,5,27,28,29,30,31,
            32,1,15,9,0,2,1,16,17,18,
            19,20,21,7,10,6,8,22,23,24,
            25,26,27,28,29,30,31,32,15,5,
            3,0,2,11,13,12,14,0,2,4,
            3,9,1,33,34,35,5,24,25,28,
            18,17,27,26,22,23,8,32,6,7,
            10,21,30,29,20,19,16,31,15,0
        };
    };
    public final static byte asr[] = Asr.asr;
    public final int asr(int index) { return asr[index]; }

    public interface Nasb {
        public final static byte nasb[] = {0,
            45,23,1,35,35,35,35,35,35,35,
            35,23,27,39,50,50,23,50,51,51,
            51,30,51,37,42,55,27,62,27,27,
            33,53,58,23,64,66,68,66,23,66,
            66,23,66,23,62,23,66,56,24,56,
            70,56,72,72,23,74,74,47,23,61,
            61,23,66,23,23,72,72,61,61
        };
    };
    public final static byte nasb[] = Nasb.nasb;
    public final int nasb(int index) { return nasb[index]; }

    public interface Nasr {
        public final static byte nasr[] = {0,
            32,53,30,29,52,27,26,25,50,45,
            22,43,46,47,18,36,16,15,14,13,
            33,24,0,39,40,0,1,2,0,1,
            44,0,56,55,1,0,51,0,1,34,
            0,1,48,0,10,0,42,41,0,1,
            3,0,54,0,1,5,0,57,1,0,
            7,2,0,35,0,4,0,37,0,49,
            0,6,0,8,0
        };
    };
    public final static byte nasr[] = Nasr.nasr;
    public final int nasr(int index) { return nasr[index]; }

    public interface TerminalIndex {
        public final static byte terminalIndex[] = {0,
            29,28,30,16,21,13,11,14,35,12,
            31,33,32,34,36,5,6,7,8,9,
            10,15,17,18,19,20,22,23,24,25,
            26,27,2,3,4,37,38,39,40
        };
    };
    public final static byte terminalIndex[] = TerminalIndex.terminalIndex;
    public final int terminalIndex(int index) { return terminalIndex[index]; }

    public interface NonterminalIndex {
        public final static byte nonterminalIndex[] = {0,
            38,38,38,38,38,38,38,38,38,38,
            38,38,38,38,38,38,38,38,38,38,
            38,38,38,38,38,38,38,38,38,38,
            38,38,38,38,38,38,38,38,38,38,
            38,38,38,38,38,38,38,38,38,38,
            38,38,38,38,38,38,38,0
        };
    };
    public final static byte nonterminalIndex[] = NonterminalIndex.nonterminalIndex;
    public final int nonterminalIndex(int index) { return nonterminalIndex[index]; }
    public final static int scopePrefix[] = null;
    public final int scopePrefix(int index) { return 0;}

    public final static int scopeSuffix[] = null;
    public final int scopeSuffix(int index) { return 0;}

    public final static int scopeLhs[] = null;
    public final int scopeLhs(int index) { return 0;}

    public final static int scopeLa[] = null;
    public final int scopeLa(int index) { return 0;}

    public final static int scopeStateSet[] = null;
    public final int scopeStateSet(int index) { return 0;}

    public final static int scopeRhs[] = null;
    public final int scopeRhs(int index) { return 0;}

    public final static int scopeState[] = null;
    public final int scopeState(int index) { return 0;}

    public final static int inSymb[] = null;
    public final int inSymb(int index) { return 0;}


    public interface Name {
        public final static String name[] = {
            "",
            "$empty",
            "DROPSYMBOLS_KEY",
            "DROPACTIONS_KEY",
            "DROPRULES_KEY",
            "NOTICE_KEY",
            "AST_KEY",
            "GLOBALS_KEY",
            "DEFINE_KEY",
            "TERMINALS_KEY",
            "KEYWORDS_KEY",
            "EOL_KEY",
            "EOF_KEY",
            "ERROR_KEY",
            "IDENTIFIER_KEY",
            "ALIAS_KEY",
            "EMPTY_KEY",
            "START_KEY",
            "TYPES_KEY",
            "RULES_KEY",
            "NAMES_KEY",
            "END_KEY",
            "HEADERS_KEY",
            "TRAILERS_KEY",
            "EXPORT_KEY",
            "IMPORT_KEY",
            "INCLUDE_KEY",
            "RECOVER_KEY",
            "MACRO_NAME",
            "SYMBOL",
            "BLOCK",
            "EQUIVALENCE",
            "PRIORITY_EQUIVALENCE",
            "ARROW",
            "PRIORITY_ARROW",
            "OR",
            "EOF",
            "EOL",
            "ERROR",
            "COMMENT",
            "OPTION_LINE"
        };
    };
    public final static String name[] = Name.name;
    public final String name(int index) { return name[index]; }

    public final static int
           ERROR_SYMBOL      = 37,
           SCOPE_UBOUND      = -1,
           SCOPE_SIZE        = 0,
           MAX_NAME_LENGTH   = 20;

    public final int getErrorSymbol() { return ERROR_SYMBOL; }
    public final int getScopeUbound() { return SCOPE_UBOUND; }
    public final int getScopeSize() { return SCOPE_SIZE; }
    public final int getMaxNameLength() { return MAX_NAME_LENGTH; }

    public final static int
           NUM_STATES        = 69,
           NT_OFFSET         = 39,
           LA_STATE_OFFSET   = 484,
           MAX_LA            = 3,
           NUM_RULES         = 149,
           NUM_NONTERMINALS  = 58,
           NUM_SYMBOLS       = 97,
           SEGMENT_SIZE      = 8192,
           START_STATE       = 198,
           IDENTIFIER_SYMBOL = 0,
           EOFT_SYMBOL       = 15,
           EOLT_SYMBOL       = 15,
           ACCEPT_ACTION     = 334,
           ERROR_ACTION      = 335;

    public final static boolean BACKTRACK = false;

    public final int getNumStates() { return NUM_STATES; }
    public final int getNtOffset() { return NT_OFFSET; }
    public final int getLaStateOffset() { return LA_STATE_OFFSET; }
    public final int getMaxLa() { return MAX_LA; }
    public final int getNumRules() { return NUM_RULES; }
    public final int getNumNonterminals() { return NUM_NONTERMINALS; }
    public final int getNumSymbols() { return NUM_SYMBOLS; }
    public final int getSegmentSize() { return SEGMENT_SIZE; }
    public final int getStartState() { return START_STATE; }
    public final int getStartSymbol() { return lhs[0]; }
    public final int getIdentifierSymbol() { return IDENTIFIER_SYMBOL; }
    public final int getEoftSymbol() { return EOFT_SYMBOL; }
    public final int getEoltSymbol() { return EOLT_SYMBOL; }
    public final int getAcceptAction() { return ACCEPT_ACTION; }
    public final int getErrorAction() { return ERROR_ACTION; }
    public final boolean isValidForParser() { return isValidForParser; }
    public final boolean getBacktrack() { return BACKTRACK; }

    public final int originalState(int state) {
        return -baseCheck[state];
    }
    public final int asi(int state) {
        return asb[originalState(state)];
    }
    public final int nasi(int state) {
        return nasb[originalState(state)];
    }
    public final int inSymbol(int state) {
        return inSymb[originalState(state)];
    }

    public final int ntAction(int state, int sym) {
        return baseAction[state + sym];
    }

    public final int tAction(int state, int sym) {
        int i = baseAction[state],
            k = i + sym;
        return termAction[termCheck[k] == sym ? k : i];
    }
    public final int lookAhead(int la_state, int sym) {
        int k = la_state + sym;
        return termAction[termCheck[k] == sym ? k : la_state];
    }
}
