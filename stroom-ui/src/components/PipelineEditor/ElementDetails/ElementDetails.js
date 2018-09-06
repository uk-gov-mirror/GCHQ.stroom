import React from 'react';
import PropTypes from 'prop-types';

import { compose, branch, renderComponent } from 'recompose';

import { connect } from 'react-redux';

import { Container, Message, Image, Form } from 'semantic-ui-react';

import { reduxForm } from 'redux-form';

import { getParentProperty } from '../pipelineUtils';

import HorizontalPanel from 'components/HorizontalPanel';

import ElementField from './ElementField';

const enhance = compose(
  connect(
    ({ pipelineEditor: { pipelineStates, elements } }, { pipelineId }) => {
      const pipelineState = pipelineStates[pipelineId];
      let initialValues;
      let selectedElementId;
      if (pipelineState) {
        initialValues = pipelineState.selectedElementInitialValues;
        selectedElementId = pipelineState.selectedElementId;
      }
      const form = `${pipelineId}-elementDetails`;

      return {
        // for our component
        elements,
        selectedElementId,
        pipelineState,
        // for redux-form
        form,
        initialValues,
      };
    },
    {
      // actions
    },
  ),
  reduxForm(),
  branch(
    ({ selectedElementId }) => !selectedElementId,
    renderComponent(() => (
      <Container className="element-details">
        <Message>
          <Message.Header>Please select an element</Message.Header>
        </Message>
      </Container>
    )),
  ),
);

const ElementDetails = ({
  pipelineId,
  pipelineState: { pipeline },
  selectedElementId,
  elements,
  onClose,
}) => {
  // These next few lines involve extracting the relevant properties from the pipeline.
  // The types of the properties and their values are in different places.
  const element = pipeline.merged.elements.add.find(element => element.id === selectedElementId);
  const elementType = elements.elements.find(e => e.type === element.type);
  const elementTypeProperties = elements.elementProperties[element.type];
  const sortedElementTypeProperties = Object.values(elementTypeProperties).sort((a, b) => a.displayPriority > b.displayPriority);

  const elementProperties = pipeline.merged.properties.add.filter(property => property.element === selectedElementId);

  const elementPropertiesInChild = pipeline.configStack[
    pipeline.configStack.length - 1
  ].properties.add.filter(property => property.element === selectedElementId);

  const title = (
    <div className="element-details__title">
      <Image
        size="small"
        src={require(`../images/${elementType.icon}`)}
        className="element-details__icon"
      />
      <div>
        <strong>{element.id}</strong>
      </div>
    </div>
  );

  const content = (
    <React.Fragment>
      <p>
        This element is a <strong>{element.type}</strong>.
      </p>
      <Form className="element-details__form">
        {Object.keys(elementTypeProperties).length === 0 ? (
          <p>There is nothing to configure for this element </p>
        ) : (
          sortedElementTypeProperties.map((elementTypeProperty) => {
            const docRefTypes = elementTypeProperty.docRefTypes
              ? elementTypeProperty.docRefTypes
              : undefined;

            const parentValue = getParentProperty(
              pipeline.configStack,
              element.id,
              elementTypeProperty.name,
            );
            const defaultValue = elementTypeProperty.defaultValue;
            const property = elementProperties.find(element => element.name === elementTypeProperty.name);
            const childProperty = elementPropertiesInChild.find(element => element.name === elementTypeProperty.name);
            return (
              <ElementField
                pipelineId={pipelineId}
                elementId={element.id}
                key={elementTypeProperty.name}
                name={elementTypeProperty.name}
                type={elementTypeProperty.type}
                docRefTypes={docRefTypes}
                description={elementTypeProperty.description}
                defaultValue={defaultValue}
                parentValue={parentValue}
                childValue={childProperty}
                value={property}
              />
            );
          })
        )}
      </Form>
    </React.Fragment>
  );

  return (
    <HorizontalPanel
      className="element-details__panel"
      title={title}
      onClose={() => onClose()}
      content={content}
      titleColumns={6}
      menuColumns={10}
      headerSize="h3"
    />
  );
};

ElementDetails.propTypes = {
  // Set by owner
  pipelineId: PropTypes.string.isRequired,
  onClose: PropTypes.func,
};

export default enhance(ElementDetails);