package org.omnetpp.ned.editor.text.highlight;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.omnetpp.common.editor.text.NedCompletionHelper;

/**
 * A rule based PrivatedNedDoc scanner.
 */
public class NedPrivateDocColorizerScanner extends RuleBasedScanner {

    /**
	 * Create a new neddoc scanner for the given color provider.
	 * 
	 * @param provider the color provider
	 */
	 public NedPrivateDocColorizerScanner() {
		super();
        // this is the default token for a comment
        setDefaultReturnToken(NedCompletionHelper.docPrivateDefaultToken);

		List<IRule> list= new ArrayList<IRule>();
        
        // Add word rule for private comment hints
        WordRule hintRule= new WordRule(NedCompletionHelper.nedWordDetector, Token.UNDEFINED);
        for (int i= 0; i < NedCompletionHelper.highlightPrivateDocTodo.length; i++)
            hintRule.addWord(NedCompletionHelper.highlightPrivateDocTodo[i], NedCompletionHelper.docPrivateTodoToken);
        list.add(hintRule);

		IRule[] result= new IRule[list.size()];
		list.toArray(result);
		setRules(result);
	}
}
