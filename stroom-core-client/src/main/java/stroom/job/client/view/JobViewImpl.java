/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.job.client.view;

import stroom.job.client.presenter.JobPresenter;
import stroom.widget.layout.client.view.ResizeSimplePanel;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class JobViewImpl extends ViewImpl implements JobPresenter.JobView {

    private final Widget widget;
    @UiField
    ResizeSimplePanel jobList;
    @UiField
    ResizeSimplePanel jobNodeList;

    @Inject
    public JobViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setInSlot(final Object slot, final Widget content) {
        if (JobPresenter.JOB_LIST.equals(slot)) {
            jobList.setWidget(content);
        } else if (JobPresenter.JOB_NODE_LIST.equals(slot)) {
            jobNodeList.setWidget(content);
        }
    }

    public interface Binder extends UiBinder<Widget, JobViewImpl> {

    }
}
