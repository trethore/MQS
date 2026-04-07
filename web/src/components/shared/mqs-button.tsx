import * as React from 'react';

import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type MqsButtonProps = React.ComponentProps<typeof Button>;

function MqsButton({ className, variant = 'default', ...props }: MqsButtonProps) {
  return (
    <Button
      variant={variant}
      className={cn(
        variant === 'outline' &&
          'border-border focus-visible:border-border dark:border-input dark:focus-visible:border-input',
        className
      )}
      {...props}
    />
  );
}

export { MqsButton };
