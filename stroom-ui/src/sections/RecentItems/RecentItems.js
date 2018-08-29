/*
 * Copyright 2018 Crown Copyright
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
import React from 'react';
import { compose } from 'recompose';
import { connect } from 'react-redux';
import { Header, Icon, Grid } from 'semantic-ui-react';
import { withRouter } from 'react-router-dom';

import AppSearchBar from 'components/AppSearchBar';
import { DocRefListingEntry } from 'components/DocRefListingEntry';
import withSelectableItemListing from 'lib/withSelectableItemListing';
import openDocRef from './openDocRef';

const LISTING_ID = 'recent-items';

const enhance = compose(
  withRouter,
  connect(
    ({ recentItems }, props) => ({
      recentItems,
    }),
    { openDocRef },
  ),
  withSelectableItemListing(({ openDocRef, history, recentItems }) => ({
    listingId: LISTING_ID,
    openItem: d => openDocRef(history, d),
    items: recentItems,
  })),
);

const RecentItems = ({ recentItems, openDocRef, onKeyDownWithShortcuts }) => (
  <React.Fragment>
    <Grid className="content-tabs__grid">
      <Grid.Column width={16}>
        <AppSearchBar />
      </Grid.Column>
      <Grid.Column width={16}>
        <Header as="h3">
          <Icon name="file outline" />
          <Header.Content>Recent Items</Header.Content>
        </Header>
      </Grid.Column>
    </Grid>
    <div className="doc-ref-listing" tabIndex={0} onKeyDown={onKeyDownWithShortcuts}>
      {recentItems.map((docRef, index) => (
        <DocRefListingEntry
          key={docRef.uuid}
          index={index}
          listingId={LISTING_ID}
          docRef={docRef}
          openDocRef={openDocRef}
          includeBreadcrumb
        />
      ))}
    </div>
  </React.Fragment>
);

export default enhance(RecentItems);
