package com.octetstring.vde.util;




import com.octetstring.vde.Credentials;



import com.octetstring.vde.Entry;



import com.octetstring.vde.chain.Chain;



import com.octetstring.vde.chain.ChainException;



import com.octetstring.vde.chain.VirtualServicesInterface;



import com.octetstring.vde.syntax.DirectoryString;



import com.octetstring.vde.syntax.Syntax;



import com.octetstring.vde.util.DirectoryException;



import com.octetstring.vde.util.PluginUtil;



import com.octetstring.vde.util.VDELogger;



import java.util.HashMap;



import java.util.List;



import java.util.Vector;




public class ActivationUtil {



    public static final DirectoryString MS_AD_ACCOUNTLOCK = new DirectoryString("userAccountControl");



    private static final DirectoryString MS_ADAM_ACCOUNTLOCK = new DirectoryString("msDS-UserAccountDisabled");



    private static final DirectoryString NOVELL_ACCOUNTLOCK = new DirectoryString("loginDisabled");



    public static final DirectoryString TRUE_VAL = new DirectoryString("TRUE");



    public static final DirectoryString FALSE_VAL = new DirectoryString("FALSE");



    public static final DirectoryString SUN_ACCOUNTLOCK = new DirectoryString("nsAccountLock");



    public static final int ORACLE_OVD_JOIN = -2;



    public static final int ORACLE_OID = -1;



    public static final int SUN_SUNONE = 0;



    public static final int MS_AD = 1;



    public static final int MS_ADAM = 2;



    public static final int NOVELL_EDIRECTORY = 3;



    public static final int ORACLE_OUD = 4;



    private PluginUtil util = PluginUtil.getInstance();



    public static VDELogger logger = VDELogger.getLogger((String)ActivationUtil.class.getName());



    public static final String build = "5320M";




    public void activateAccount(int directoryType, Entry entry, DirectoryString activationAttribute, List<DirectoryString> activeVals, List<DirectoryString> deactiveVals) {



        switch (directoryType) {



            case 0: {



                this.util.copyAdd(activationAttribute, SUN_ACCOUNTLOCK, entry);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueAdd(SUN_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)FALSE_VAL, entry);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueAdd(SUN_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)TRUE_VAL, entry);



                }



                break;



            }



            case 3: {



                this.util.copyAdd(activationAttribute, NOVELL_ACCOUNTLOCK, entry);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueAdd(NOVELL_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)FALSE_VAL, entry);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueAdd(NOVELL_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)TRUE_VAL, entry);



                }



                break;



            }



            case 2: {



                this.util.copyAdd(activationAttribute, MS_ADAM_ACCOUNTLOCK, entry);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueAdd(MS_ADAM_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)FALSE_VAL, entry);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueAdd(MS_ADAM_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)TRUE_VAL, entry);



                }



                break;



            }



            case 1: {



                this.util.copyAdd(activationAttribute, MS_AD_ACCOUNTLOCK, entry);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueAdd(MS_AD_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)new DirectoryString("544"), entry);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueAdd(MS_AD_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)new DirectoryString("546"), entry);



                }



                break;



            }



        }



   }




    public void activateAccount(int directoryType, Vector changes, Chain chain, Credentials creds, DirectoryString name, DirectoryString activationAttribute, List activeVals, List deactiveVals) throws ChainException, DirectoryException {



        this.activateAccount(directoryType, changes, chain, creds, name, activationAttribute, activeVals, deactiveVals, null);



    }




    public void activateAccount(int directoryType, Vector changes, Chain chain, Credentials creds, DirectoryString name, DirectoryString activationAttribute, List<DirectoryString> activeVals, List<DirectoryString> deactiveVals, Entry entry) throws ChainException, DirectoryException {



        switch (directoryType) {



            case 0: {



                this.util.copyModify(activationAttribute, SUN_ACCOUNTLOCK, changes);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueModify(SUN_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)FALSE_VAL, changes);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueModify(SUN_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)TRUE_VAL, changes);



                }



                break;



            }



            case 3: {



                this.util.copyModify(activationAttribute, NOVELL_ACCOUNTLOCK, changes);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueModify(NOVELL_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)FALSE_VAL, changes);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueModify(NOVELL_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)TRUE_VAL, changes);



                }



                break;



            }



            case 2: {



                this.util.copyModify(activationAttribute, MS_ADAM_ACCOUNTLOCK, changes);



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueModify(MS_ADAM_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)FALSE_VAL, changes);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueModify(MS_ADAM_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)TRUE_VAL, changes);



                }



                break;



            }



            case 1: {



                this.util.copyModify(activationAttribute, MS_AD_ACCOUNTLOCK, changes);



                if (entry == null) {



                    entry = chain.getVSI().getByDn(chain.getRequest(), creds, name);



                }



                if (entry.get(MS_AD_ACCOUNTLOCK) == null) {



                    return;



                }



                int userAccountControl = Integer.parseInt(((Syntax)entry.get(MS_AD_ACCOUNTLOCK).get(0)).toString());



                for (DirectoryString activatedValue : activeVals) {



                    this.util.revalueModify(MS_AD_ACCOUNTLOCK, (Syntax)activatedValue, (Syntax)new DirectoryString(Integer.toString(userAccountControl & -3)), changes);



                }



                for (DirectoryString deactivatedValue : deactiveVals) {



                    this.util.revalueModify(MS_AD_ACCOUNTLOCK, (Syntax)deactivatedValue, (Syntax)new DirectoryString(Integer.toString(userAccountControl | 2)), changes);



                }



                break;



            }



        }



    }




    static {



        logger.info("OVD-20201", new Object[]{"5320M"});



    }



}

