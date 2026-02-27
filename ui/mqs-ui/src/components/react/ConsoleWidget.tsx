import { ConsoleCommandInputWidget } from "@/components/react/ConsoleCommandInputWidget";
import { ConsoleOutputWidget } from "@/components/react/ConsoleOutputWidget";
import { useConsoleController } from "@/hooks/useConsoleController";

export function ConsoleWidget() {
  const {
    errorMessage,
    executeInput,
    executing,
    handleInputKeyDown,
    inputValue,
    loading,
    setCommandInput,
    snapshot,
  } = useConsoleController();

  return (
    <div className="flex min-h-0 flex-1 flex-col">
      {errorMessage ? (
        <p
          role="alert"
          className="mb-3 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive"
        >
          {errorMessage}
        </p>
      ) : null}

      <ConsoleOutputWidget loading={loading} messages={snapshot.messages} />

      <ConsoleCommandInputWidget
        disabled={loading || executing}
        inputValue={inputValue}
        onInputKeyDown={handleInputKeyDown}
        onInputValueChange={setCommandInput}
        onSubmit={executeInput}
      />
    </div>
  );
}
