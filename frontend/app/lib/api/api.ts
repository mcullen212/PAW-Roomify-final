import axios from "axios";
import Qs from "qs";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 5000,
    paramsSerializer: params => Qs.stringify(params, {arrayFormat: 'repeat'})
});

export default api;
