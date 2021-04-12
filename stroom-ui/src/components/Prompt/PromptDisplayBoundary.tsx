import * as React from "react";
import {
  createContext,
  FunctionComponent,
  useCallback,
  useContext,
  useState,
} from "react";
import Prompt, { ContentProps, Props, PromptType } from "./Prompt";

type PromptContextType = {
  props: Props;
  setProps: (value: Props) => void;
};

const PromptContext = createContext<PromptContextType | undefined>(undefined);

export interface Api {
  showPrompt: (props: Props) => void;
  showInfo: (props: ContentProps) => void;
  showWarning: (props: ContentProps) => void;
  showError: (props: ContentProps) => void;
  showFatal: (props: ContentProps) => void;
}

export const usePrompt = (): Api => {
  const { setProps } = useContext(PromptContext);

  const showPrompt = useCallback(
    (props: Props) => {
      setProps(props);
    },
    [setProps],
  );

  const showInfo = useCallback(
    ({ title = "Info", message = "" }) => {
      showPrompt({
        promptProps: {
          type: PromptType.INFO,
          title: title,
          message: message,
        },
      });
    },
    [showPrompt],
  );

  const showWarning = useCallback(
    ({ title = "Warning", message = "" }) => {
      showPrompt({
        promptProps: {
          type: PromptType.WARNING,
          title: title,
          message: message,
        },
      });
    },
    [showPrompt],
  );

  const showError = useCallback(
    ({ title = "Error", message = "" }) => {
      showPrompt({
        promptProps: {
          type: PromptType.ERROR,
          title: title,
          message: message,
        },
      });
    },
    [showPrompt],
  );

  const showFatal = useCallback(
    ({ title = "Fatal", message = "" }) => {
      showPrompt({
        promptProps: {
          type: PromptType.FATAL,
          title: title,
          message: message,
        },
      });
    },
    [showPrompt],
  );

  return { showPrompt, showInfo, showWarning, showError, showFatal };
};

const PromptOutlet: FunctionComponent = () => {
  const { props, setProps } = useContext(PromptContext);
  console.log("Render: Prompt");
  if (props !== undefined) {
    return (
      <Prompt
        promptProps={props.promptProps}
        onCloseDialog={() => {
          if (props.onCloseDialog) {
            props.onCloseDialog();
          }
          setProps(undefined);
        }}
      />
    );
  } else {
    return null;
  }
};

export const PromptDisplayBoundary: FunctionComponent = ({ children }) => {
  const [props, setProps] = useState<Props | undefined>(undefined);
  return (
    <PromptContext.Provider value={{ props, setProps }}>
      {children}
      <PromptOutlet />
    </PromptContext.Provider>
  );
};
