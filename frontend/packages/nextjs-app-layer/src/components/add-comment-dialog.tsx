"use client";

import { useState } from "react";
import { Send } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  MarkdownEditor,
  Button,
} from "@open-elements/ui";

interface AddCommentDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly onSubmit: (text: string) => Promise<void>;
  readonly sending: boolean;
  readonly title: string;
  readonly placeholder: string;
  readonly sendLabel: string;
  readonly sendingLabel: string;
  readonly errorTitle: string;
  readonly errorMessage: string;
}

export function AddCommentDialog({
  open,
  onOpenChange,
  onSubmit,
  sending,
  title,
  placeholder,
  sendLabel,
  sendingLabel,
  errorTitle,
  errorMessage,
}: AddCommentDialogProps) {
  const [text, setText] = useState("");
  const [errorOpen, setErrorOpen] = useState(false);

  const isTextEmpty = !text.trim();

  const handleSubmit = async () => {
    if (isTextEmpty || sending) return;
    try {
      await onSubmit(text.trim());
      setText("");
      onOpenChange(false);
    } catch {
      setErrorOpen(true);
    }
  };

  const handleOpenChange = (value: boolean) => {
    if (!value) {
      setText("");
    }
    onOpenChange(value);
  };

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="font-heading">{title}</DialogTitle>
          </DialogHeader>
          <MarkdownEditor
            value={text}
            onChange={setText}
            placeholder={placeholder}
          />
          <DialogFooter>
            <Button onClick={handleSubmit} disabled={isTextEmpty || sending}>
              <Send className="mr-2 h-4 w-4" />
              {sending ? sendingLabel : sendLabel}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={errorOpen} onOpenChange={setErrorOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{errorTitle}</AlertDialogTitle>
            <AlertDialogDescription>{errorMessage}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>OK</AlertDialogCancel>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
