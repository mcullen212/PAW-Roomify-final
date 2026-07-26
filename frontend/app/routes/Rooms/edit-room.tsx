import { useState } from "react"
import { useNavigate, useParams, useSearchParams } from "react-router"
import { useTranslation } from "react-i18next"
import { Navbar } from "@/components/Navbar"
import { EditRoomForm } from "~/components/rooms/edit-room-form.tsx"
import type { RoomUpdatePayload } from "@/lib/interfaces/rooms"
import { toast } from "sonner"
import { readReturnTo } from "@/lib/navigation"
import { pageTitleKey } from "@/lib/utils";
import { useApiServices } from "~/lib/hooks/useApiServices";
import { getApiErrorMessage } from "~/lib/api/api-error-message"
import NotFound from "~/routes/errors/not-found-page"
import { getApiErrorPage } from "~/routes/errors/api-error-page"

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.editRoom") },
        { name: "description", content: pageTitleKey("pageDescriptions.editRoom") },
    ]
}

export default function EditRoomPage() {
    const { t } = useTranslation()
    const { id } = useParams()
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const returnTo = readReturnTo(searchParams, "/my-rooms")
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [error, setError] = useState("")
    const { roomService } = useApiServices()
    const roomId = id ? Number(id) : undefined
    const roomQuery = roomService.useGetRoomById(roomId)
    const updateRoomMutation = roomService.useUpdateRoom()
    const room = roomQuery.data ?? null

    const handleSave = async (payload: RoomUpdatePayload) => {
        if (!roomId) return

        setError("")
        setIsSubmitting(true)

        try {
            await updateRoomMutation.mutateAsync({ roomId, roomData: payload })
            toast.success(t("editRoom.success.updated"))
            navigate(returnTo)
        } catch (err: any) {
            setError(err?.response?.data?.message || t("editRoom.errors.update"))
        } finally {
            setIsSubmitting(false)
        }
    }

    if (roomQuery.isLoading) {
        return (
            <div className="min-h-screen bg-muted/30">
                <Navbar />
                <main className="mx-auto flex max-w-4xl items-center justify-center px-4 py-24">
                    <p className="text-muted-foreground">{t("editRoom.loading")}</p>
                </main>
            </div>
        )
    }

    const apiErrorPage = getApiErrorPage(roomQuery.error, {
        badRequest: !roomId,
        notFoundTitleKey: "editRoom.notFound.title",
        notFoundDescriptionKey: "editRoom.notFound.description",
    })
    if (apiErrorPage) return apiErrorPage
    if (roomQuery.isError) {
        return (
            <div className="min-h-screen bg-muted/30">
                <Navbar />
                <main className="mx-auto flex max-w-3xl flex-col items-center px-4 py-20 text-center sm:px-6 lg:px-8">
                    <p className="text-red-500">{getApiErrorMessage(roomQuery.error, t("editRoom.errors.load"))}</p>
                </main>
            </div>
        )
    }
    if (!room) return <NotFound titleKey="editRoom.notFound.title" descriptionKey="editRoom.notFound.description" />

    return (
        <div className="min-h-screen bg-muted/30">
            <Navbar />
            <main className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
                <EditRoomForm
                    room={room}
                    isSubmitting={isSubmitting}
                    error={error}
                    backTo={returnTo}
                    onSave={handleSave}
                />
            </main>
        </div>
    )
}
