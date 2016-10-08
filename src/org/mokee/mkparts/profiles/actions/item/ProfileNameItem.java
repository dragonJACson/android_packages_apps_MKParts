/*
 * Copyright (C) 2014 The MoKee Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mokee.mkparts.profiles.actions.item;

import org.mokee.mkparts.profiles.actions.ItemListAdapter;

import mokee.app.Profile;

public class ProfileNameItem extends BaseItem {
    Profile mProfile;

    public ProfileNameItem(Profile profile) {
        this.mProfile = profile;
    }

    @Override
    public ItemListAdapter.RowType getRowType() {
        return ItemListAdapter.RowType.NAME_ITEM;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getTitle() {
        return mProfile.getName();
    }

    @Override
    public String getSummary() {
        return null;
    }
}
