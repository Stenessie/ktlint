package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.ruleset.core.api.ElementType.GT
import com.pinterest.ktlint.ruleset.core.api.ElementType.RBRACKET
import com.pinterest.ktlint.ruleset.core.api.ElementType.RPAR
import com.pinterest.ktlint.ruleset.core.api.isPartOfString
import com.pinterest.ktlint.ruleset.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.ruleset.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.core.api.nextSibling
import com.pinterest.ktlint.ruleset.core.api.prevCodeLeaf
import com.pinterest.ktlint.ruleset.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.core.api.upsertWhitespaceAfterMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

public class SpacingAroundCommaRule : Rule("comma-spacing") {
    private val rTokenSet = TokenSet.create(RPAR, RBRACKET, GT)

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node is LeafPsiElement && node.textMatches(",") && !node.isPartOfString()) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf is PsiWhiteSpace) {
                emit(prevLeaf.startOffset, "Unexpected spacing before \"${node.text}\"", true)
                if (autoCorrect) {
                    val isPrecededByComment = prevLeaf.prevLeaf { it !is PsiWhiteSpace } is PsiComment
                    if (isPrecededByComment && prevLeaf.isWhiteSpaceWithNewline()) {
                        // If comma is on new line and preceded by a comment, it should be moved before this comment
                        // https://github.com/pinterest/ktlint/issues/367
                        val previousStatement = node.prevCodeLeaf()!!
                        previousStatement.treeParent.addChild(node.clone(), previousStatement.nextSibling { true })
                        val nextLeaf = node.nextLeaf()
                        if (nextLeaf is PsiWhiteSpace) {
                            nextLeaf.treeParent.removeChild(nextLeaf)
                        }
                        node.treeParent.removeChild(node)
                    } else {
                        prevLeaf.treeParent.removeChild(prevLeaf)
                    }
                }
            }
            val nextLeaf = node.nextLeaf()
            if (nextLeaf !is PsiWhiteSpace && nextLeaf?.elementType !in rTokenSet) {
                emit(node.startOffset + 1, "Missing spacing after \"${node.text}\"", true)
                if (autoCorrect) {
                    (node as ASTNode).upsertWhitespaceAfterMe(" ")
                }
            }
        }
    }
}
