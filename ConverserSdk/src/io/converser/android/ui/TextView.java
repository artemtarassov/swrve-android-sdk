package io.converser.android.ui;

import android.content.Context;

import io.converser.android.engine.model.Content;
import io.converser.android.engine.model.ConversationAtom;

public class TextView extends android.widget.TextView implements ConverserContent {

    private Content model;

    public TextView(Context context, Content model) {
        super(context);

        this.model = model;

        init(model);
    }

    public TextView(Context context, Content model, int style) {
        super(context, null, style);
        this.model = model;
        init(model);
    }

    private void init(Content model) {
        if (model.getType().equals(ConversationAtom.TYPE_CONTENT_TEXT)) {
            setText(model.getValue());
        } else {
            setText(model.getValue());
        }


    }


    @Override
    public Content getModel() {
        return model;
    }

}
