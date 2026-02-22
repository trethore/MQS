import { useState } from "react";

import { Button } from "@/components/ui/button";

export function HelloWorldToggle() {
  const [showMessage, setShowMessage] = useState(false);

  return (
    <div className="flex flex-1 items-center justify-center px-6 py-8">
      <div className="flex flex-col items-center gap-3">
        <Button onClick={() => setShowMessage((previous) => !previous)}>
          {showMessage ? "Hide message" : "Show message"}
        </Button>
        {showMessage ? <p className="text-sm text-muted-foreground">Hello world</p> : null}
      </div>
    </div>
  );
}
