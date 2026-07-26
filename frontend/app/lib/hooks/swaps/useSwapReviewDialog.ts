import { useCallback, useState } from "react";
import { toast } from "sonner";
import type { Contact } from "~/lib/interfaces/contacts";

type Translate = (key: string, options?: Record<string, unknown>) => string;

interface CreateReviewMutation {
    mutateAsync: (payload: {
        contactId: number;
        reviewerId: number;
        rating: number;
        comment: string;
    }) => Promise<unknown>;
}

interface UseSwapReviewDialogOptions {
    createReviewMutation: CreateReviewMutation;
    reviewerId?: number;
    onReviewCreated: () => Promise<void>;
    t: Translate;
}

const validCommentPattern = /^[\p{L}\p{N}\s\p{P}]*$/u;

export function useSwapReviewDialog({
    createReviewMutation,
    reviewerId,
    onReviewCreated,
    t,
}: UseSwapReviewDialogOptions) {
    const [contact, setContact] = useState<Contact | null>(null);
    const [rating, setRating] = useState<number | null>(null);
    const [comment, setComment] = useState("");
    const [error, setError] = useState<string | null>(null);
    const [submittingContactId, setSubmittingContactId] = useState<number | null>(null);

    const reset = useCallback(() => {
        setContact(null);
        setRating(null);
        setComment("");
        setError(null);
    }, []);

    const open = useCallback((selectedContact: Contact) => {
        setContact(selectedContact);
        setRating(null);
        setComment("");
        setError(null);
    }, []);

    const close = useCallback(() => {
        if (!submittingContactId) {
            reset();
        }
    }, [reset, submittingContactId]);

    const handleOpenChange = useCallback((openDialog: boolean) => {
        if (!openDialog) {
            close();
        }
    }, [close]);

    const handleRatingChange = useCallback((selectedRating: number) => {
        setRating(selectedRating);
        setError(null);
    }, []);

    const handleCommentChange = useCallback((nextComment: string) => {
        setComment(nextComment);
        setError(null);
    }, []);

    const submit = useCallback(async () => {
        if (!contact || submittingContactId) {
            return;
        }

        if (!reviewerId) {
            setError(t("reviewActions.error"));
            return;
        }

        const trimmedComment = comment.trim();

        if (!rating || rating < 1 || rating > 5) {
            setError(t("reviewActions.validation.rating"));
            return;
        }

        if (!trimmedComment) {
            setError(t("reviewActions.validation.comment"));
            return;
        }

        if (trimmedComment.length > 500) {
            setError(t("reviewActions.validation.commentLength"));
            return;
        }

        if (!validCommentPattern.test(trimmedComment)) {
            setError(t("reviewActions.validation.commentPattern"));
            return;
        }

        setSubmittingContactId(contact.id);
        setError(null);

        try {
            await createReviewMutation.mutateAsync({
                contactId: contact.id,
                reviewerId,
                rating,
                comment: trimmedComment,
            });
            reset();
            toast.success(t("reviewActions.success"));
            await onReviewCreated();
        } catch (submitError: any) {
            setError(submitError?.response?.data?.message || t("reviewActions.error"));
        } finally {
            setSubmittingContactId(null);
        }
    }, [
        comment,
        contact,
        createReviewMutation,
        onReviewCreated,
        rating,
        reviewerId,
        reset,
        submittingContactId,
        t,
    ]);

    return {
        contact,
        rating,
        comment,
        error,
        submittingContactId,
        open,
        handleOpenChange,
        handleRatingChange,
        handleCommentChange,
        submit,
    };
}
