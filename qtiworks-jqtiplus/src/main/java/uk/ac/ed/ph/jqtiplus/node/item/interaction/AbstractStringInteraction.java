/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.jqtiplus.node.item.interaction;

import uk.ac.ed.ph.jqtiplus.attribute.value.IdentifierAttribute;
import uk.ac.ed.ph.jqtiplus.attribute.value.IntegerAttribute;
import uk.ac.ed.ph.jqtiplus.attribute.value.StringAttribute;
import uk.ac.ed.ph.jqtiplus.exception2.ResponseBindingException;
import uk.ac.ed.ph.jqtiplus.node.XmlNode;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.item.response.declaration.ResponseDeclaration;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.validation.ValidationContext;
import uk.ac.ed.ph.jqtiplus.validation.ValidationError;
import uk.ac.ed.ph.jqtiplus.validation.ValidationWarning;
import uk.ac.ed.ph.jqtiplus.value.BaseType;
import uk.ac.ed.ph.jqtiplus.value.IntegerValue;
import uk.ac.ed.ph.jqtiplus.value.RecordValue;
import uk.ac.ed.ph.jqtiplus.value.Value;

import java.util.List;

/**
 * Base class for {@link TextEntryInteraction} and {@link ExtendedTextInteraction}
 * 
 * @author David McKain
 */
public abstract class AbstractStringInteraction extends InlineInteraction implements StringInteraction {

    private static final long serialVersionUID = 1113644056576463196L;
    
    public AbstractStringInteraction(XmlNode parent, String localName) {
        super(parent, localName);

        //for StringInteraction...
        getAttributes().add(new IntegerAttribute(this, ATTR_BASE_NAME, ATTR_BASE_DEFAULT_VALUE, ATTR_BASE_DEFAULT_VALUE, false));
        getAttributes().add(new IdentifierAttribute(this, ATTR_STRING_IDENTIFIER_NAME, null, null, false));
        getAttributes().add(new IntegerAttribute(this, ATTR_EXPECTED_LENGTH_NAME, null, null, false));
        getAttributes().add(new StringAttribute(this, ATTR_PATTERN_MASK_NAME, null, null, false));
        getAttributes().add(new StringAttribute(this, ATTR_PLACEHOLDER_TEXT_NAME, null, null, false));
    }

    @Override
    public final List<? extends XmlNode> getChildren() {
        return null;
    }

    @Override
    public Integer getBase() {
        return getAttributes().getIntegerAttribute(ATTR_BASE_NAME).getComputedValue();
    }

    @Override
    public Integer getExpectedLength() {
        return getAttributes().getIntegerAttribute(ATTR_EXPECTED_LENGTH_NAME).getComputedValue();
    }

    @Override
    public String getPatternMask() {
        return getAttributes().getStringAttribute(ATTR_PATTERN_MASK_NAME).getComputedValue();
    }

    @Override
    public String getPlaceholderText() {
        return getAttributes().getStringAttribute(ATTR_PLACEHOLDER_TEXT_NAME).getComputedValue();
    }

    @Override
    public Identifier getStringIdentifier() {
        return getAttributes().getIdentifierAttribute(ATTR_STRING_IDENTIFIER_NAME).getComputedValue();
    }

    @Override
    public void setBase(Integer base) {
        getAttributes().getIntegerAttribute(ATTR_BASE_NAME).setValue(base);
    }

    @Override
    public void setExpectedLength(Integer expectedLength) {
        getAttributes().getIntegerAttribute(ATTR_EXPECTED_LENGTH_NAME).setValue(expectedLength);
    }

    @Override
    public void setPatternMask(String patternMask) {
        getAttributes().getStringAttribute(ATTR_PATTERN_MASK_NAME).setValue(patternMask);
    }

    @Override
    public void setPlaceholderText(String placeholderText) {
        getAttributes().getStringAttribute(ATTR_PLACEHOLDER_TEXT_NAME).setValue(placeholderText);
    }

    @Override
    public void setStringIdentifier(Identifier stringIdentifier) {
        getAttributes().getIdentifierAttribute(ATTR_STRING_IDENTIFIER_NAME).setValue(stringIdentifier);
    }

    @Override
    public ResponseDeclaration getStringIdentifierResponseDeclaration() {
        if (getStringIdentifier() == null) {
            return null;
        }
        return getRootObject(AssessmentItem.class).getResponseDeclaration(getStringIdentifier());
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        if (getResponseIdentifier() != null) {
            final ResponseDeclaration declaration = getResponseDeclaration();
            if (declaration != null && declaration.getCardinality() != null
                    && !(declaration.getCardinality().isSingle() || declaration.getCardinality().isRecord())) {
                context.add(new ValidationError(this, "Response variable must have single or record cardinality"));
            }

            if (declaration != null && declaration.getCardinality() != null && !declaration.getCardinality().isRecord()) {
                if (!(declaration.getBaseType().isString() || declaration.getBaseType().isNumeric())) {
                    context.add(new ValidationError(this, "Response variable must have string or numeric base type"));
                }
            }

            if (declaration != null && declaration.getBaseType() != null && !declaration.getBaseType().isFloat() && getBase() != 10) {
                context.add(new ValidationWarning(this, "JQTI currently doesn't support radix conversion for floats. Base attribute will be ignored."));
            }
        }

        if (getStringIdentifier() != null) {
            final ResponseDeclaration declaration = getStringIdentifierResponseDeclaration();

            if (declaration != null && declaration.getBaseType() != null && !declaration.getBaseType().isString()) {
                context.add(new ValidationError(this, "StringIdentifier response variable must have String base type"));
            }
        }
    }

    @Override
    public void bindResponse(ItemSessionController itemController, List<String> responseList) throws ResponseBindingException {
        super.bindResponse(itemController, responseList);

        /* Also handle stringIdentifier binding if required */
        if (getStringIdentifier() != null) {
            final ResponseDeclaration stringIdentifierResponseDeclaration = getStringIdentifierResponseDeclaration();
            final Value value = bindResponse(stringIdentifierResponseDeclaration, responseList);
            itemController.getItemState().setResponseValue(this, value);
        }
    }

    protected final RecordValue bindRecordValueResponse(String responseString) {
        final RecordValue value = new RecordValue();

        value.add(KEY_STRING_VALUE_NAME, BaseType.STRING.parseSingleValue(responseString));
        value.add(KEY_FLOAT_VALUE_NAME, BaseType.FLOAT.parseSingleValue(responseString));

        String exponentIndicator = null;
        if (responseString.contains("e")) {
            exponentIndicator = "e";
        }
        if (responseString.contains("E")) {
            exponentIndicator = "E";
        }

        final String exponentPart = exponentIndicator != null ? responseString.substring(responseString.indexOf(exponentIndicator) + 1) : null;
        responseString = exponentIndicator == null ? responseString : responseString.substring(0, responseString.indexOf(exponentIndicator));
        final String rightPart = responseString.contains(".") ? responseString.substring(responseString.indexOf(".") + 1) : null;
        final String leftPart = responseString.contains(".") ? responseString.substring(0, responseString.indexOf(".")) : responseString;

        if (exponentIndicator != null || responseString.contains(".")) {
            value.add(KEY_INTEGER_VALUE_NAME, null);
        }
        else {
            value.add(KEY_INTEGER_VALUE_NAME, new IntegerValue(responseString, getBase()));
        }

        value.add(KEY_LEFT_DIGITS_NAME, new IntegerValue(leftPart == null ? 0 : leftPart.length()));
        value.add(KEY_RIGHT_DIGITS_NAME, new IntegerValue(rightPart == null ? 0 : rightPart.length()));

        if (exponentIndicator != null) {
            int frac = rightPart == null || rightPart.length() == 0 ? 0 : rightPart.length();
            if (exponentPart != null && exponentPart.length() > 0) {
                frac -= Integer.parseInt(exponentPart);
            }

            value.add(KEY_NDP_NAME, new IntegerValue(frac));
        }
        else {
            value.add(KEY_NDP_NAME, new IntegerValue(rightPart == null || rightPart.length() == 0 ? "0" : rightPart));
        }

        int nsf = leftPart == null || leftPart.length() == 0 ? 0 : new Integer(leftPart).toString().length();
        nsf += rightPart == null || rightPart.length() == 0 ? 0 : rightPart.length();
        value.add(KEY_NSF_NAME, new IntegerValue(nsf));

        if (exponentIndicator != null) {
            value.add(KEY_EXPONENT_NAME, new IntegerValue(exponentPart!=null && exponentPart.length() == 0 ? "0" : exponentPart));
        }
        else {
            value.add(KEY_EXPONENT_NAME, null);
        }
        return value;
    }

    @Override
    public boolean validateResponse(ItemSessionController itemController, Value responseValue) {
        if (getPatternMask() != null) {
            if (!responseValue.toString().matches(getPatternMask())) {
                return false;
            }
        }

        return true;
    }
}