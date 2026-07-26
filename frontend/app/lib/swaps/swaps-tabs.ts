import {
    CheckCircle2,
    Clock3,
    History,
    Inbox,
    Send,
    XCircle,
} from "lucide-react";
import type { SwapTabConfig, SwapTabId } from "~/lib/interfaces/swaps";

export const swapTabs: SwapTabConfig[] = [
    {
        id: "sent",
        label: "swaps.tabs.sent.label",
        title: "swaps.tabs.sent.title",
        emptyMessage: "swaps.tabs.sent.emptyMessage",
        icon: Send,
    },
    {
        id: "received",
        label: "swaps.tabs.received.label",
        title: "swaps.tabs.received.title",
        emptyMessage: "swaps.tabs.received.emptyMessage",
        icon: Inbox,
    },
    {
        id: "active",
        label: "swaps.tabs.active.label",
        title: "swaps.tabs.active.title",
        emptyMessage: "swaps.tabs.active.emptyMessage",
        icon: CheckCircle2,
    },
    {
        id: "canceled",
        label: "swaps.tabs.canceled.label",
        title: "swaps.tabs.canceled.title",
        emptyMessage: "swaps.tabs.canceled.emptyMessage",
        icon: XCircle,
    },
    {
        id: "past",
        label: "swaps.tabs.past.label",
        title: "swaps.tabs.past.title",
        emptyMessage: "swaps.tabs.past.emptyMessage",
        icon: History,
    },
    {
        id: "expired",
        label: "swaps.tabs.expired.label",
        title: "swaps.tabs.expired.title",
        emptyMessage: "swaps.tabs.expired.emptyMessage",
        icon: Clock3,
    },
];

export function getSwapTabFromSearch(searchParams: URLSearchParams): SwapTabId {
    const view = searchParams.get("view");

    if (view && swapTabs.some((tab) => tab.id === view)) {
        return view as SwapTabId;
    }

    return "sent";
}
