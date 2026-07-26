import {useEffect, useState} from "react"
import { useTranslation } from "react-i18next"
import {Navbar} from "@/components/Navbar"
import {RoomCard} from "~/components/rooms/RoomCard.tsx"
import {useLocation, useNavigate} from "react-router";
import {PagingBar} from "@/components/PagingBar";
import { Card } from "@/components/ui/card";
import { ConfirmDeleteModal } from "~/components/modals/confirm-delete-modal.tsx"
import { useAuth } from "~/lib/auth/useAuth"
import { currentPath, withReturnTo } from "~/lib/navigation"
import { pageTitleKey } from "@/lib/utils";
import { useApiServices } from "~/lib/hooks/useApiServices";
import type { Room } from "@/types";
import { getPaginationWithFallback } from "~/lib/pagination";
import { useNormalizePaginationPage, usePaginationParams } from "~/lib/hooks/usePaginationParams";
import { getApiErrorMessage } from "@/lib/api/api-error-message";
import { getApiErrorPage } from "~/routes/errors/api-error-page";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.myRooms") },
        { name: "description", content: pageTitleKey("pageDescriptions.myRooms") },
    ]
}

export default function RoomsPage() {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const location = useLocation()
    const { currentPage, setCurrentPage } = usePaginationParams()
    const { userId } = useAuth()
    const { roomService } = useApiServices()

    const roomsQuery = roomService.useGetMyRooms(userId ?? undefined, currentPage, 100)
    const deleteRoom = roomService.useDeleteRoom()
    const rooms: Room[] = roomsQuery.data?.data ?? []
    const pagination = getPaginationWithFallback(roomsQuery.data?.pagination, currentPage)
    useNormalizePaginationPage(roomsQuery.data?.pagination, currentPage, setCurrentPage)

    const [isDeleteOpen, setIsDeleteOpen] = useState(false)
    const [roomToDelete, setRoomToDelete] = useState<Room | null>(null)
    const [deleteError, setDeleteError] = useState("")

    const handleDeleteClick = (room: Room) => {
        setDeleteError("")
        setRoomToDelete(room)
        setIsDeleteOpen(true)
    }

    const handleEditClick = (room: any) => {
        navigate(withReturnTo(`/room/${room.id}/edit`, currentPath(location)))
    }

    useEffect(() => {
        if (!userId) {
            navigate("/login")
        }
    }, [navigate, userId])

    const handleConfirmDelete = async () => {
        if (!roomToDelete) return
        setDeleteError("")
        try {
            await deleteRoom.mutateAsync(roomToDelete.id)
            setIsDeleteOpen(false)
            setRoomToDelete(null)
        } catch (err) {
            setDeleteError(getApiErrorMessage(err, t("myRooms.delete.errors.failed")))
        }
    }

    const handleDeleteOpenChange = (open: boolean) => {
        setIsDeleteOpen(open)
        if (!open) {
            setDeleteError("")
            setRoomToDelete(null)
        }
    }

    const apiErrorPage = getApiErrorPage(roomsQuery.error, {
        notFoundTitleKey: "profile.userNotFound",
        notFoundDescriptionKey: "error404.userDescription",
    })
    if (apiErrorPage) return apiErrorPage
    if (roomsQuery.isError) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <main className="mx-auto flex max-w-3xl flex-col items-center px-4 py-20 text-center">
                    <p className="text-red-500">{getApiErrorMessage(roomsQuery.error, t("myRooms.errors.load"))}</p>
                </main>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-background">
            <Navbar/>

            <ConfirmDeleteModal
                open={isDeleteOpen}
                onOpenChange={handleDeleteOpenChange}
                onConfirm={handleConfirmDelete}
                title={t("myRooms.delete.title")}
                description={t("myRooms.delete.description")}
                itemName={roomToDelete?.title}
                isLoading={deleteRoom.isPending}
                errorMessage={deleteError}
            />

            <div className="bg-gradient-to-r from-[#3b82f6] to-[#60a5fa] py-12 px-4 sm:px-6 lg:px-8">
                <div className="max-w-7xl mx-auto">
                    <h1 className="text-4xl font-bold text-white italic mb-2">{t("myRooms.title")}</h1>
                    <p className="text-white/90 text-lg">
                        {t("myRooms.subtitle")}
                    </p>
                </div>
            </div>

            {/* Rooms Grid */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
                <Card className="overflow-hidden border border-border bg-card py-0 shadow-sm">
                    <div className="px-5 py-8 sm:px-8">
                        {rooms.length === 0 ? (
                            <div className="text-center py-16">
                                <p className="text-muted-foreground text-lg">
                                    {t("myRooms.empty")}
                                </p>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                {rooms.map((room) => (
                                    <RoomCard
                                        key={room.id}
                                        room={room}
                                        onDelete={handleDeleteClick}
                                        onEdit={handleEditClick}
                                    />
                                ))}
                            </div>
                        )}

                        <PagingBar
                            currentPage={pagination.currentPage}
                            totalPages={pagination.totalPages}
                            links={pagination.links}
                            onPageChange={setCurrentPage}
                        />
                    </div>
                </Card>
            </main>
        </div>
    )
}
